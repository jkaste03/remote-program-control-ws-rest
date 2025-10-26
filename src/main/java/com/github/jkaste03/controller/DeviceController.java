package com.github.jkaste03.controller;

import java.util.List;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.jkaste03.dto.DeviceDto;
import com.github.jkaste03.security.SecurityUtils;
import com.github.jkaste03.service.DeviceService;

@RestController
@RequestMapping("/remote-program-control/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public List<DeviceDto> getDevicesForCurrentUser(Authentication authentication) {

        Set<String> userRoleNames = SecurityUtils.extractRoleNames(authentication);
        return deviceService.listDevicesForRoles(userRoleNames);
    }
}
