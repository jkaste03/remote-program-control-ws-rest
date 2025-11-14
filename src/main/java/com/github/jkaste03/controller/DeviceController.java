package com.github.jkaste03.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.github.jkaste03.dto.request.DeviceCreateRequest;
import com.github.jkaste03.dto.response.DeviceResponse;
import com.github.jkaste03.security.SecurityUtils;
import com.github.jkaste03.service.DeviceService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.github.jkaste03.security.UserPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/remote-program-control/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public List<DeviceResponse> getDevicesUserCanInteractWith(Authentication authentication,
            @AuthenticationPrincipal UserPrincipal principal) {

        String userRoleName = SecurityUtils.extractRoleName(authentication);
        boolean isAdmin = userRoleName.equals("ADMIN");

        long userId = principal.id();
        return deviceService.getDevicesUserCanInteractWith(userId, isAdmin);
    }

    @GetMapping("/{id}")
    public DeviceResponse getDeviceDetails(@PathVariable long id, Authentication authentication,
            @AuthenticationPrincipal UserPrincipal principal) {
        long userId = principal.id();

        return deviceService.getDeviceDetails(id, userId);
    }

    @PostMapping
    public ResponseEntity<DeviceResponse> createDevice(@Valid @RequestBody DeviceCreateRequest createRequest,
            @AuthenticationPrincipal UserPrincipal principal) {

        long userId = principal.id();
        DeviceResponse device = deviceService.createDevice(createRequest, userId);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest() // /api/v1/devices
                .path("/{id}")
                .buildAndExpand(device.id())
                .toUri();

        return ResponseEntity.created(location).body(device);
    }
}
