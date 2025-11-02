package com.github.jkaste03.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.jkaste03.dto.DeviceDto;
import com.github.jkaste03.dto.ProgramDto;
import com.github.jkaste03.model.Device;
import com.github.jkaste03.model.Program;
import com.github.jkaste03.repository.DeviceRepository;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    /**
     * Henter devices for bruker. Hvis admin=true, returneres alle devices med alle
     * programmer.
     * Ellers:
     * - Eierskap: alle programmer
     * - Ikke-eierskap: bare programmer som er public ELLER hvor brukeren er allowed
     */
    @Transactional(readOnly = true)
    public List<DeviceDto> getDevicesForUser(Long userId, boolean isAdmin) {
        final List<Device> devices;
        if (isAdmin) {
            devices = deviceRepository.findAllWithPrograms();
        } else {
            devices = deviceRepository.findDevicesUserCanInteractWithPrefetched(userId);
        }

        List<DeviceDto> result = new ArrayList<>(devices.size());
        for (Device d : devices) {
            boolean isOwner = d.getUser().getId().equals(userId);

            List<ProgramDto> programDtos = d.getPrograms()
                    .stream()
                    .filter(p -> filterProgramForUser(p, userId, isOwner, isAdmin))
                    .map(p -> new ProgramDto(p.getId(), p.getName(), p.isPublic()))
                    .toList();

            result.add(new DeviceDto(d.getId(), d.getName(), d.getUser().getUsername(), programDtos));
        }
        return result;
    }

    private boolean filterProgramForUser(Program p, Long userId, boolean isOwner, boolean isAdmin) {
        if (isAdmin || isOwner)
            return true;
        if (p.isPublic())
            return true;
        return p.getAllowedUsers().stream().anyMatch(u -> u.getId().equals(userId));
    }
}
