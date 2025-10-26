package com.github.jkaste03.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.github.jkaste03.dto.DeviceDto;
import com.github.jkaste03.dto.ProgramDto;
import com.github.jkaste03.repository.DeviceRepository;
import com.github.jkaste03.view.DeviceProgramView;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepo;

    public DeviceService(DeviceRepository deviceRepo) {
        this.deviceRepo = deviceRepo;
    }

    public List<DeviceDto> listDevicesForRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty())
            return List.of();
        Set<String> norm = roleNames.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

        List<DeviceProgramView> rows = deviceRepo.findDevicesWithProgramsByRoleNames(norm);

        Map<Long, DeviceDto> acc = new LinkedHashMap<>();
        for (DeviceProgramView r : rows) {
            DeviceDto dto = acc.computeIfAbsent(r.getDeviceId(),
                    id -> new DeviceDto(id, r.getDeviceName(), new ArrayList<>()));
            dto.programs().add(new ProgramDto(
                    r.getProgramId(),
                    r.getProgramName(),
                    r.getRoleName()));
        }
        return new ArrayList<>(acc.values()); // bare devices med minst ett matchende program
    }
}
