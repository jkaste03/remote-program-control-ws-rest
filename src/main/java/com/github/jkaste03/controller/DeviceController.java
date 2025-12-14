package com.github.jkaste03.controller;

import java.net.URI;
import java.util.List;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.jkaste03.dto.request.DeviceCreateRequest;
import com.github.jkaste03.dto.request.ProgramUpdateRequest;
import com.github.jkaste03.dto.request.ProgramSyncRequest;
import com.github.jkaste03.dto.response.DeviceResponse;
import com.github.jkaste03.controller.hateoas.DeviceModelAssembler;
import com.github.jkaste03.security.AppRoles;
import com.github.jkaste03.security.UserPrincipal;
import com.github.jkaste03.service.DeviceService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/remote-program-control/api/v1/devices")
@Validated
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceModelAssembler deviceModelAssembler;

    public DeviceController(DeviceService deviceService, DeviceModelAssembler deviceModelAssembler) {
        this.deviceService = deviceService;
        this.deviceModelAssembler = deviceModelAssembler;
    }

    @GetMapping
    public CollectionModel<EntityModel<DeviceResponse>> getDevicesUserCanInteractWith(Authentication authentication,
            @AuthenticationPrincipal UserPrincipal principal) {

        boolean isAdmin = isAdmin(authentication);

        List<DeviceResponse> devices = deviceService.getDevicesUserCanInteractWith(principal.id(), isAdmin);
        return deviceModelAssembler.toCollectionModel(devices);
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(AppRoles.ROLE_ADMIN));
    }

    @GetMapping("/{id}")
    public EntityModel<DeviceResponse> getDeviceDetails(@PathVariable long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return deviceModelAssembler.toModel(deviceService.getDeviceDetails(id, principal.id()));
    }

    @PostMapping
    public ResponseEntity<EntityModel<DeviceResponse>> createDevice(
            @Valid @RequestBody DeviceCreateRequest createRequest,
            @AuthenticationPrincipal UserPrincipal principal) {

        DeviceResponse device = deviceService.createDevice(createRequest, principal);
        EntityModel<DeviceResponse> model = deviceModelAssembler.toModel(device);

        URI location = model.getRequiredLink(IanaLinkRelations.SELF.value()).toUri();

        return ResponseEntity.created(location).body(model);
    }

    @PutMapping("/{id}")
    public EntityModel<DeviceResponse> updateDevice(@PathVariable long id,
            @Valid @RequestBody DeviceCreateRequest updateRequest,
            @AuthenticationPrincipal UserPrincipal principal) {

        DeviceResponse device = deviceService.updateDevice(id, updateRequest, principal.id());
        return deviceModelAssembler.toModel(device);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        deviceService.deleteDevice(id, principal.id());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/programs")
    public EntityModel<DeviceResponse> addProgramToDevice(@PathVariable long id,
            @Valid @RequestBody ProgramUpdateRequest createRequest, @AuthenticationPrincipal UserPrincipal principal) {

        DeviceResponse device = deviceService.addProgramToDevice(id, createRequest, principal.id());
        return deviceModelAssembler.toModel(device);
    }

    @PutMapping("/{id}/programs/sync")
    public ResponseEntity<Void> syncPrograms(@PathVariable long id,
            @RequestBody @NotEmpty @Valid List<ProgramSyncRequest> programs,
            @AuthenticationPrincipal UserPrincipal principal) {

        deviceService.syncPrograms(id, programs, principal.id());
        return ResponseEntity.ok().build();
    }
}
