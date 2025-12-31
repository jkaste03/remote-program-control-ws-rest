package com.github.jkaste03.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.jkaste03.dto.request.DeviceCreateRequest;
import com.github.jkaste03.dto.request.ProgramCreateUpdateRequest;
import com.github.jkaste03.dto.request.ProgramSyncRequest;
import com.github.jkaste03.dto.response.DeviceResponse;
import com.github.jkaste03.dto.response.ProgramResponse;
import com.github.jkaste03.event.DeviceStatusChangedEvent;
import com.github.jkaste03.exceptions.ConflictException;
import com.github.jkaste03.exceptions.ForbiddenException;
import com.github.jkaste03.exceptions.ResourceNotFoundException;
import com.github.jkaste03.model.Device;
import com.github.jkaste03.model.Program;
import com.github.jkaste03.model.User;
import com.github.jkaste03.repository.DeviceRepository;
import com.github.jkaste03.repository.ProgramRepository;
import com.github.jkaste03.repository.UserRepository;
import com.github.jkaste03.security.UserPrincipal;
import com.github.jkaste03.security.UserProvisioningService;

@Service
public class DeviceService {

    private static final int STANDARD_WOL_PORT = 9;
    private static final String PROBLEM_NOT_FOUND_DEVICE = "problem.not-found.device";
    private static final String PROBLEM_FORBIDDEN_DEVICE_ACCESS = "problem.forbidden.device-access";
    private static final String PROBLEM_OWNER_NAME_CONFLICT = "device.conflict.owner-name";
    private static final String PROBLEM_OWNER_MAC_ADDRESS_CONFLICT = "device.conflict.owner-mac-address";
    private static final String PROBLEM_NOT_FOUND_USERS = "problem.not-found.users";

    private final DeviceRepository deviceRepository;
    private final ProgramRepository programRepository;
    private final ProgramResponseMapper programResponseMapper;
    private final ProgramUniquenessValidator programUniquenessValidator;
    private final ProgramAccessService programAccessService;
    private final UserProvisioningService userProvisioningService;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    // DO NOT INJECT SSESERVICE TO AVOID CIRCULAR REFERENCES

    public DeviceService(DeviceRepository deviceRepository, UserProvisioningService userProvisioningService,
            ProgramRepository programRepository, ProgramResponseMapper programResponseMapper,
            ProgramUniquenessValidator programUniquenessValidator, ProgramAccessService programAccessService,
            UserRepository userRepository, ApplicationEventPublisher eventPublisher) {
        this.deviceRepository = deviceRepository;
        this.userProvisioningService = userProvisioningService;
        this.programRepository = programRepository;
        this.programResponseMapper = programResponseMapper;
        this.programUniquenessValidator = programUniquenessValidator;
        this.programAccessService = programAccessService;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Henter devices for bruker. Hvis admin=true, returneres alle devices med alle
     * programmer.
     * Ellers:
     * - Eierskap: alle programmer
     * - Ikke-eierskap: bare programmer som er public ELLER hvor brukeren er allowed
     */
    @Transactional(readOnly = true)
    public @NonNull List<DeviceResponse> getDevicesUserCanInteractWith(UUID userId, boolean isAdmin) {
        List<Device> devices = isAdmin
                ? deviceRepository.findAllWithPrograms()
                : deviceRepository.findDevicesUserCanInteractWithPrefetched(userId);

        List<DeviceResponse> deviceResponses = devices.stream()
                .map(device -> {
                    List<ProgramResponse> programResponses = device.getPrograms().stream()
                            .filter(program -> isAdmin || programAccessService.hasAccess(program, userId))
                            .map(programResponseMapper::toSummary)
                            .toList();

                    return new DeviceResponse(device.getId(), device.getName(), device.getOwner().getUsername(),
                            device.getOwner().getId(), programResponses, device.getStatus(), device.getLastSeen(),
                            device.getCreatedAt());
                })
                .toList();

        return Objects.requireNonNull(deviceResponses);
    }

    @Transactional
    public @NonNull DeviceResponse createDevice(DeviceCreateRequest createRequest, @NonNull UserPrincipal principal) {
        String deviceName = createRequest.name();
        String macAddress = createRequest.macAddress();
        String publicIp = createRequest.publicIp();
        User owner = userProvisioningService.getOrCreateUser(principal);
        UUID userId = principal.id();

        ensureUniqueIdentifiers(userId, deviceName, macAddress, null);

        int wolPort = resolveWakeOnLanPort(createRequest.wakeOnLanPort());
        Device device = new Device(deviceName, publicIp, wolPort, macAddress, owner);
        Device savedDevice = deviceRepository.save(device);

        return toDeviceDetailsResponse(savedDevice, false);
    }

    @Transactional(readOnly = true)
    public @NonNull DeviceResponse getDeviceDetails(long id, UUID userId, boolean includePrograms) {
        Device device = requireOwnedDevice(id, userId);
        return toDeviceDetailsResponse(device, includePrograms);
    }

    @Transactional
    public @NonNull DeviceResponse updateDevice(long id, DeviceCreateRequest updateRequest, UUID userId) {
        Device device = requireOwnedDevice(id, userId);

        String newName = updateRequest.name();
        String newMacAddress = updateRequest.macAddress();

        ensureUniqueIdentifiers(userId, newName, newMacAddress, device);

        device.updateDeviceDetails(newName, updateRequest.publicIp(),
                resolveWakeOnLanPort(updateRequest.wakeOnLanPort()), newMacAddress);

        return toDeviceDetailsResponse(device, false);
    }

    @Transactional
    public void deleteDevice(long id, UUID userId, boolean isAdmin) {
        Device device = getDeviceWithAccessControl(id, userId, isAdmin);
        deviceRepository.delete(device);
    }

    private void ensureUniqueIdentifiers(UUID ownerId, String name, String macAddress, Device currentDevice) {
        boolean checkName = currentDevice == null || !currentDevice.getName().equals(name);
        if (checkName && deviceRepository.existsByOwnerIdAndName(ownerId, name)) {
            throw new ConflictException(PROBLEM_OWNER_NAME_CONFLICT, name);
        }

        boolean checkMacAddress = currentDevice == null || !currentDevice.getMacAddress().equals(macAddress);
        if (checkMacAddress && deviceRepository.existsByOwnerIdAndMacAddress(ownerId, macAddress)) {
            throw new ConflictException(PROBLEM_OWNER_MAC_ADDRESS_CONFLICT, macAddress);
        }
    }

    public @NonNull Device requireOwnedDevice(long deviceId, UUID userId) {
        return getDeviceWithAccessControl(deviceId, userId, false);
    }

    // Felles metode som håndterer all tilgangskontroll for enheter
    private @NonNull Device getDeviceWithAccessControl(long deviceId, UUID userId, boolean isAdmin) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException(PROBLEM_NOT_FOUND_DEVICE, deviceId));

        if (!isAdmin && !Objects.equals(device.getOwner().getId(), userId)) {
            throw new ForbiddenException(PROBLEM_FORBIDDEN_DEVICE_ACCESS, deviceId);
        }
        return device;
    }

    private @NonNull DeviceResponse toDeviceDetailsResponse(Device device, boolean includePrograms) {
        List<ProgramResponse> includedPrograms = null;
        if (includePrograms) {
            includedPrograms = device.getPrograms().stream()
                    .map(programResponseMapper::toSummary)
                    .toList();
        }

        User owner = device.getOwner();
        return new DeviceResponse(device.getId(), device.getName(), owner.getUsername(), owner.getId(),
                includedPrograms, device.getStatus(), device.getLastSeen(), device.getCreatedAt(), device.getPublicIp(),
                device.getWakeOnLanPort(), device.getMacAddress());
    }

    private int resolveWakeOnLanPort(Integer requestedPort) {
        return Optional.ofNullable(requestedPort).orElse(STANDARD_WOL_PORT);
    }

    @Transactional
    public @NonNull ProgramResponse addProgramToDeviceReturningProgram(long id,
            ProgramCreateUpdateRequest createRequest,
            UUID userId) {
        Device device = requireOwnedDevice(id, userId);

        programUniquenessValidator.requireUniqueNameAndFilenameForDevice(device, createRequest.filename(),
                createRequest.name(), null);

        Program program = new Program(createRequest.filename(), createRequest.name(), createRequest.isPublic());

        // Håndter eventuelle allowedUserIds
        if (!createRequest.allowedUserIds().isEmpty()) {
            List<User> foundUsers = userRepository.findAllById(createRequest.allowedUserIds());

            // HER: Sjekk om vi fant alle, ellers kast feil
            if (foundUsers.size() != createRequest.allowedUserIds().size()) {
                throw new ResourceNotFoundException(PROBLEM_NOT_FOUND_USERS);
            }

            // Legg til rettighetene
            foundUsers.forEach(program::allow);
        }

        device.addProgram(program);
        programRepository.save(program);

        return programResponseMapper.toSummary(program);
    }

    @Transactional
    public @NonNull DeviceResponse syncPrograms(long deviceId, List<ProgramSyncRequest> detectedPrograms, UUID userId) {
        Device device = requireOwnedDevice(deviceId, userId);

        // 1. Lag et Map av eksisterende programmer for O(1) oppslag
        Map<String, Program> existingProgramsMap = device.getPrograms().stream()
                .collect(Collectors.toMap(Program::getFilename, Function.identity()));

        // 2. Gå gjennom det som finnes på PC-en (Legg til nye)
        for (ProgramSyncRequest req : detectedPrograms) {
            if (!existingProgramsMap.containsKey(req.filename())) {
                // Programmet finnes ikke fra før -> Legg til
                programUniquenessValidator.requireUniqueNameAndFilenameForDevice(device, req.filename());

                Program newProgram = new Program(req.filename());
                device.addProgram(newProgram);
                programRepository.save(newProgram);
            }
        }

        // 3. Slett programmer som IKKE lenger finnes på PC-en
        // Samle opp filnavnene fra requesten i et Set for rask sjekk
        Set<String> detectedFilenames = detectedPrograms.stream()
                .map(ProgramSyncRequest::filename)
                .collect(Collectors.toSet());

        List<Program> toRemove = device.getPrograms().stream()
                .filter(p -> !detectedFilenames.contains(p.getFilename()))
                .toList();

        for (Program p : toRemove) {
            if (p == null) {
                continue;
            }
            device.removeProgram(p);
            programRepository.delete(p);
        }

        return toDeviceDetailsResponse(device, true);
    }

    @Transactional
    public void updateDeviceStatus(long deviceId, Device.Status status) {
        deviceRepository.findById(deviceId).ifPresent(device -> {
            device.setStatus(status);
            if (status == Device.Status.ONLINE) {
                device.markSeen(Instant.now());
            }
            Device savedDevice = deviceRepository.save(device);
            eventPublisher.publishEvent(new DeviceStatusChangedEvent(this, savedDevice));
        });
    }
}
