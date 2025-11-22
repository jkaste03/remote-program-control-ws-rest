package com.github.jkaste03.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.jkaste03.dto.request.DeviceCreateRequest;
import com.github.jkaste03.dto.request.ProgramCreateRequest;
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
import com.github.jkaste03.repository.UserRepository;

@Service
public class DeviceService {

    private static final int STANDARD_WOL_PORT = 9;
    private static final String PROBLEM_NOT_FOUND_USER = "problem.not-found.user";
    private static final String PROBLEM_NOT_FOUND_DEVICE = "problem.not-found.device";
    private static final String PROBLEM_FORBIDDEN_DEVICE_ACCESS = "problem.forbidden.device-access";
    private static final String PROBLEM_OWNER_NAME_CONFLICT = "device.owner-name-conflict";
    private static final String PROBLEM_OWNER_MAC_ADDRESS_CONFLICT = "device.owner-mac-address-conflict";

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final ProgramRepository programRepository;

    public DeviceService(DeviceRepository deviceRepository, UserRepository userRepository,
            ProgramRepository programRepository) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.programRepository = programRepository;
    }

    /**
     * Henter devices for bruker. Hvis admin=true, returneres alle devices med alle
     * programmer.
     * Ellers:
     * - Eierskap: alle programmer
     * - Ikke-eierskap: bare programmer som er public ELLER hvor brukeren er allowed
     */
    @Transactional(readOnly = true)
    public @NonNull List<DeviceResponse> getDevicesUserCanInteractWith(long userId, boolean isAdmin) {
        final List<Device> devices;
        if (isAdmin) {
            devices = deviceRepository.findAllWithPrograms();
        } else {
            devices = deviceRepository.findDevicesUserCanInteractWithPrefetched(userId);
        }

        List<DeviceResponse> result = new ArrayList<>(devices.size());
        for (Device d : devices) {
            boolean isOwner = d.getOwner().getId().equals(userId);

            List<ProgramResponse> programResponses = d.getPrograms()
                    .stream()
                    .filter(p -> filterProgramForUser(p, userId, isOwner, isAdmin))
                    .map(this::toProgramDetailsResponse)
                    .toList();

            result.add(new DeviceResponse(d.getId(), d.getName(), d.getOwner().getUsername(), programResponses,
                    d.getStatus(), d.getLastSeen(), d.getCreatedAt()));
        }
        return result;
    }

    private boolean filterProgramForUser(Program p, long userId, boolean isOwner, boolean isAdmin) {
        if (isAdmin || isOwner)
            return true;
        if (p.isPublic())
            return true;
        return p.getAllowedUsers().stream().anyMatch(u -> u.getId().equals(userId));
    }

    @Transactional
    public @NonNull DeviceResponse createDevice(DeviceCreateRequest createRequest, long userId) {
        String deviceName = createRequest.name();
        String macAddress = createRequest.macAddress();
        String publicIp = createRequest.publicIp();
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(PROBLEM_NOT_FOUND_USER, userId));

        ensureUniqueIdentifiers(userId, deviceName, macAddress, null);

        int wolPort = resolveWakeOnLanPort(createRequest.wakeOnLanPort());
        Device device = new Device(deviceName, publicIp, wolPort, macAddress, owner);
        Device savedDevice = deviceRepository.save(device);

        return toDeviceDetailsResponse(savedDevice, false);
    }

    @Transactional(readOnly = true)
    public @NonNull DeviceResponse getDeviceDetails(long id, long userId) {
        Device device = requireOwnedDevice(id, userId);
        return toDeviceDetailsResponse(device, false);
    }

    @Transactional
    public @NonNull DeviceResponse updateDevice(long id, DeviceCreateRequest updateRequest, long userId) {
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
    public void deleteDevice(long id, long userId) {
        Device device = requireOwnedDevice(id, userId);
        deviceRepository.delete(device);
    }

    private void ensureUniqueIdentifiers(long ownerId, String name, String macAddress, Device currentDevice) {
        boolean checkName = currentDevice == null || !currentDevice.getName().equals(name);
        if (checkName && deviceRepository.existsByOwnerIdAndName(ownerId, name)) {
            throw new ConflictException(PROBLEM_OWNER_NAME_CONFLICT, name);
        }

        boolean checkMacAddress = currentDevice == null || !currentDevice.getMacAddress().equals(macAddress);
        if (checkMacAddress && deviceRepository.existsByOwnerIdAndMacAddress(ownerId, macAddress)) {
            throw new ConflictException(PROBLEM_OWNER_MAC_ADDRESS_CONFLICT, macAddress);
        }
    }

    private @NonNull Device requireOwnedDevice(long deviceId, long userId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException(PROBLEM_NOT_FOUND_DEVICE, deviceId));

        if (!device.getOwner().getId().equals(userId)) {
            throw new ForbiddenException(PROBLEM_FORBIDDEN_DEVICE_ACCESS, deviceId);
        }
        return device;
    }

    private @NonNull DeviceResponse toDeviceDetailsResponse(Device device, boolean includePrograms) {
        List<ProgramResponse> includedPrograms = null;
        if (includePrograms) {
            includedPrograms = device.getPrograms().stream()
                    .map(this::toProgramDetailsResponse)
                    .toList();
        }

        User owner = device.getOwner();
        return new DeviceResponse(device.getId(), device.getName(), owner.getUsername(), includedPrograms,
                device.getStatus(), device.getLastSeen(), device.getCreatedAt(),
                device.getPublicIp(), device.getWakeOnLanPort(), device.getMacAddress());
    }

    private ProgramResponse toProgramDetailsResponse(Program program) {
        return new ProgramResponse(program.getId(), program.getName(), program.isPublic(), null);
    }

    private int resolveWakeOnLanPort(Integer requestedPort) {
        return Optional.ofNullable(requestedPort).orElse(STANDARD_WOL_PORT);
    }

    @Transactional
    public @NonNull DeviceResponse addProgramToDevice(long id, ProgramCreateRequest createRequest, long userId) {
        Device device = requireOwnedDevice(id, userId);

        Program program = new Program(createRequest.name(), createRequest.isPublic());
        device.addProgram(program);
        programRepository.save(program);

        return toDeviceDetailsResponse(device, true);
    }
}
