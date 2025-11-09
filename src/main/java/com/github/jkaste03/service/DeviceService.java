package com.github.jkaste03.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.jkaste03.dto.request.DeviceCreateRequest;
import com.github.jkaste03.dto.response.DeviceResponse;
import com.github.jkaste03.dto.response.ProgramResponse;
import com.github.jkaste03.exceptions.ConflictException;
import com.github.jkaste03.exceptions.ResourceNotFoundException;
import com.github.jkaste03.model.Device;
import com.github.jkaste03.model.Program;
import com.github.jkaste03.model.User;
import com.github.jkaste03.repository.DeviceRepository;
import com.github.jkaste03.repository.UserRepository;

@Service
public class DeviceService {

    private static final int STANDARD_WOL_PORT = 9;

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    public DeviceService(DeviceRepository deviceRepository, UserRepository userRepository) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
    }

    /**
     * Henter devices for bruker. Hvis admin=true, returneres alle devices med alle
     * programmer.
     * Ellers:
     * - Eierskap: alle programmer
     * - Ikke-eierskap: bare programmer som er public ELLER hvor brukeren er allowed
     */
    @Transactional(readOnly = true)
    public List<DeviceResponse> getDevicesForUser(long userId, boolean isAdmin) {
        final List<Device> devices;
        if (isAdmin) {
            devices = deviceRepository.findAllWithPrograms();
        } else {
            devices = deviceRepository.findDevicesUserCanInteractWithPrefetched(userId);
        }

        List<DeviceResponse> result = new ArrayList<>(devices.size());
        for (Device d : devices) {
            boolean isOwner = d.getOwner().getId().equals(userId);

            List<ProgramResponse> programDtos = d.getPrograms()
                    .stream()
                    .filter(p -> filterProgramForUser(p, userId, isOwner, isAdmin))
                    .map(p -> new ProgramResponse(p.getId(), p.getName(), p.isPublic()))
                    .toList();

            result.add(new DeviceResponse(d.getId(), d.getName(), d.getOwner().getUsername(), programDtos,
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

    public DeviceResponse createDevice(DeviceCreateRequest createRequest, long userId) {
        String deviceName = createRequest.name();
        String macAddress = createRequest.macAddress();
        String publicIp = createRequest.publicIp();
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("problem.not-found.user", userId));

        if (deviceRepository.existsByOwnerIdAndName(userId, createRequest.name())) {
            throw new ConflictException("device.owner-name-conflict", createRequest.name());
        }

        if (deviceRepository.existsByOwnerIdAndMacAddress(userId, createRequest.macAddress())) {
            throw new ConflictException("device.owner-mac-address-conflict", createRequest.macAddress());
        }

        int wolPort = Optional.ofNullable(createRequest.wakeOnLanPort()).orElse(STANDARD_WOL_PORT);
        Device device = new Device(deviceName, publicIp, wolPort, macAddress, owner);
        Device savedDevice = deviceRepository.save(device);

        return new DeviceResponse(savedDevice.getId(), savedDevice.getName(), owner.getUsername(), List.of(),
                savedDevice.getStatus(), savedDevice.getLastSeen(), savedDevice.getCreatedAt(),
                savedDevice.getPublicIp(), savedDevice.getWakeOnLanPort(), savedDevice.getMacAddress());
    }
}
