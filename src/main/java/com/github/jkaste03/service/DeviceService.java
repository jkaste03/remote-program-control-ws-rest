package com.github.jkaste03.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.jkaste03.dto.request.DeviceCreateRequest;
import com.github.jkaste03.dto.request.ProgramUpdateRequest;
import com.github.jkaste03.dto.request.ProgramSyncRequest;
import com.github.jkaste03.dto.response.DeviceResponse;
import com.github.jkaste03.dto.response.ProgramResponse;
import com.github.jkaste03.exceptions.ConflictException;
import com.github.jkaste03.exceptions.ForbiddenException;
import com.github.jkaste03.exceptions.ResourceNotFoundException;
import com.github.jkaste03.model.Device;
import com.github.jkaste03.model.Program;
import com.github.jkaste03.model.User;
import com.github.jkaste03.repository.DeviceRepository;
import com.github.jkaste03.repository.ProgramRepository;
import com.github.jkaste03.security.UserPrincipal;
import com.github.jkaste03.security.UserProvisioningService;

@Service
public class DeviceService {

    private static final int STANDARD_WOL_PORT = 9;
    private static final String PROBLEM_NOT_FOUND_DEVICE = "problem.not-found.device";
    private static final String PROBLEM_FORBIDDEN_DEVICE_ACCESS = "problem.forbidden.device-access";
    private static final String PROBLEM_OWNER_NAME_CONFLICT = "device.owner-name-conflict";
    private static final String PROBLEM_OWNER_MAC_ADDRESS_CONFLICT = "device.owner-mac-address-conflict";

    private final DeviceRepository deviceRepository;
    private final ProgramRepository programRepository;
    private final ProgramResponseMapper programResponseMapper;
    private final ProgramUniquenessValidator programUniquenessValidator;
    private final UserProvisioningService userProvisioningService;

    public DeviceService(DeviceRepository deviceRepository, UserProvisioningService userProvisioningService,
            ProgramRepository programRepository, ProgramResponseMapper programResponseMapper,
            ProgramUniquenessValidator programUniquenessValidator) {
        this.deviceRepository = deviceRepository;
        this.userProvisioningService = userProvisioningService;
        this.programRepository = programRepository;
        this.programResponseMapper = programResponseMapper;
        this.programUniquenessValidator = programUniquenessValidator;
    }

    /**
     * Henter devices for bruker. Hvis admin=true, returneres alle devices med alle
     * programmer.
     * Ellers:
     * - Eierskap: alle programmer
     * - Ikke-eierskap: bare programmer som er public ELLER hvor brukeren er allowed
     */
    @Transactional(readOnly = true)
    public @NonNull List<DeviceResponse> getDevicesUserCanInteractWith(String userId, boolean isAdmin) {
        final List<Device> devices;
        if (isAdmin) {
            devices = deviceRepository.findAllWithPrograms();
        } else {
            devices = deviceRepository.findDevicesUserCanInteractWithPrefetched(userId);
        }

        List<DeviceResponse> result = new ArrayList<>(devices.size());
        for (Device d : devices) {
            boolean isOwner = Objects.equals(d.getOwner().getId(), userId);

            List<ProgramResponse> programResponses = d.getPrograms()
                    .stream()
                    .filter(p -> filterProgramForUser(p, userId, isOwner, isAdmin))
                    .map(programResponseMapper::toSummary)
                    .toList();

            result.add(new DeviceResponse(d.getId(), d.getName(), d.getOwner().getUsername(), programResponses,
                    d.getStatus(), d.getLastSeen(), d.getCreatedAt()));
        }
        return result;
    }

    private boolean filterProgramForUser(Program p, String userId, boolean isOwner, boolean isAdmin) {
        if (isAdmin || isOwner)
            return true;
        if (p.isPublic())
            return true;
        return p.getAllowedUsers().stream().anyMatch(u -> Objects.equals(u.getId(), userId));
    }

    @Transactional
    public @NonNull DeviceResponse createDevice(DeviceCreateRequest createRequest, @NonNull UserPrincipal principal) {
        String deviceName = createRequest.name();
        String macAddress = createRequest.macAddress();
        String publicIp = createRequest.publicIp();
        User owner = userProvisioningService.getOrCreateUser(principal);
        String userId = principal.id();

        ensureUniqueIdentifiers(userId, deviceName, macAddress, null);

        int wolPort = resolveWakeOnLanPort(createRequest.wakeOnLanPort());
        Device device = new Device(deviceName, publicIp, wolPort, macAddress, owner);
        Device savedDevice = deviceRepository.save(device);

        return toDeviceDetailsResponse(savedDevice, false);
    }

    @Transactional(readOnly = true)
    public @NonNull DeviceResponse getDeviceDetails(long id, String userId) {
        Device device = requireOwnedDevice(id, userId);
        return toDeviceDetailsResponse(device, false);
    }

    @Transactional
    public @NonNull DeviceResponse updateDevice(long id, DeviceCreateRequest updateRequest, String userId) {
        Device device = requireOwnedDevice(id, userId);

        String newName = updateRequest.name();
        String newMacAddress = updateRequest.macAddress();

        ensureUniqueIdentifiers(userId, newName, newMacAddress, device);

        device.setName(newName);
        device.setPublicIp(updateRequest.publicIp());
        device.setWakeOnLanPort(resolveWakeOnLanPort(updateRequest.wakeOnLanPort()));
        device.setMacAddress(newMacAddress);

        return toDeviceDetailsResponse(device, false);
    }

    @Transactional
    public void deleteDevice(long id, String userId) {
        Device device = requireOwnedDevice(id, userId);
        deviceRepository.delete(device);
    }

    private void ensureUniqueIdentifiers(String ownerId, String name, String macAddress, Device currentDevice) {
        boolean checkName = currentDevice == null || !currentDevice.getName().equals(name);
        if (checkName && deviceRepository.existsByOwnerIdAndName(ownerId, name)) {
            throw new ConflictException(PROBLEM_OWNER_NAME_CONFLICT, name);
        }

        boolean checkMacAddress = currentDevice == null || !currentDevice.getMacAddress().equals(macAddress);
        if (checkMacAddress && deviceRepository.existsByOwnerIdAndMacAddress(ownerId, macAddress)) {
            throw new ConflictException(PROBLEM_OWNER_MAC_ADDRESS_CONFLICT, macAddress);
        }
    }

    private @NonNull Device requireOwnedDevice(long deviceId, String userId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException(PROBLEM_NOT_FOUND_DEVICE, deviceId));

        if (!Objects.equals(device.getOwner().getId(), userId)) {
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
        return new DeviceResponse(device.getId(), device.getName(), owner.getUsername(), includedPrograms,
                device.getStatus(), device.getLastSeen(), device.getCreatedAt(),
                device.getPublicIp(), device.getWakeOnLanPort(), device.getMacAddress());
    }

    private int resolveWakeOnLanPort(Integer requestedPort) {
        return Optional.ofNullable(requestedPort).orElse(STANDARD_WOL_PORT);
    }

    @Transactional
    public @NonNull DeviceResponse addProgramToDevice(long id, ProgramUpdateRequest createRequest, String userId) {
        Device device = requireOwnedDevice(id, userId);

        programUniquenessValidator.requireUniqueNameAndFilenameForDevice(device, createRequest.filename(),
                createRequest.name(), null);

        Program program = new Program(createRequest.filename());
        device.addProgram(program);
        programRepository.save(program);

        return toDeviceDetailsResponse(device, true);
    }

    @Transactional
    public void syncPrograms(long deviceId, List<ProgramSyncRequest> detectedPrograms, String userId) {
        Device device = requireOwnedDevice(deviceId, userId);
        Set<Program> dbPrograms = device.getPrograms();

        // 1. Gå gjennom det som finnes på PC-en
        for (ProgramSyncRequest req : detectedPrograms) {
            Optional<Program> existing = dbPrograms.stream()
                    .filter(p -> p.getFilename().equals(req.filename()))
                    .findFirst();

            if (!existing.isPresent()) {
                programUniquenessValidator.requireUniqueNameAndFilenameForDevice(device, req.filename());
                // Programmet er nytt - legg til
                Program newProgram = new Program(req.filename());
                device.addProgram(newProgram); // Dette håndterer relasjonen i Device.java
                programRepository.save(newProgram);
            }
        }

        // 2. (Valgfritt) Slett programmer fra DB som IKKE lenger finnes på PC-en
        // Dette sikrer at web-appen ikke viser "spøkelsesprogrammer"
        List<Program> toRemove = dbPrograms.stream()
                .filter(dbP -> detectedPrograms.stream()
                        .noneMatch(req -> req.filename().equals(dbP.getFilename())))
                .toList();

        for (Program p : toRemove) {
            if (p == null) {
                continue;
            }
            device.removeProgram(p); // Fjerner fra listen i Device.java
            programRepository.delete(p);
        }
    }
}
