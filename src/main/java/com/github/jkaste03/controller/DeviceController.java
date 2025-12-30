package com.github.jkaste03.controller;

import java.net.URI;
import java.util.List;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.github.jkaste03.controller.assembler.DeviceModelAssembler;
import com.github.jkaste03.controller.assembler.ProgramModelAssembler;
import com.github.jkaste03.dto.request.DeviceCreateRequest;
import com.github.jkaste03.dto.request.ProgramCreateUpdateRequest;
import com.github.jkaste03.dto.request.ProgramSyncRequest;
import com.github.jkaste03.dto.response.DeviceResponse;
import com.github.jkaste03.dto.response.ProgramResponse;
import com.github.jkaste03.security.UserPrincipal;
import com.github.jkaste03.security.SecurityUtils;
import com.github.jkaste03.service.DeviceService;
import com.github.jkaste03.service.SseService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import org.springframework.web.bind.annotation.RequestParam;

@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/remote-program-control/api/v1/devices")
@Validated
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceModelAssembler deviceModelAssembler;
    private final ProgramModelAssembler programModelAssembler;
    private final SseService sseService;

    public DeviceController(DeviceService deviceService, DeviceModelAssembler deviceModelAssembler,
            ProgramModelAssembler programModelAssembler,
            SseService sseService) {
        this.deviceService = deviceService;
        this.deviceModelAssembler = deviceModelAssembler;
        this.programModelAssembler = programModelAssembler;
        this.sseService = sseService;
    }

    @GetMapping
    public CollectionModel<EntityModel<DeviceResponse>> getDevicesUserCanInteractWith(Authentication authentication,
            @AuthenticationPrincipal UserPrincipal principal) {

        boolean isAdmin = SecurityUtils.isAdmin(authentication);

        List<DeviceResponse> devices = deviceService.getDevicesUserCanInteractWith(principal.id(), isAdmin);
        return deviceModelAssembler.toCollectionModel(devices);
    }

    @GetMapping("/{id}")
    public EntityModel<DeviceResponse> getDeviceDetails(@PathVariable long id,
            @RequestParam(required = false, defaultValue = "false") boolean includePrograms,
            @AuthenticationPrincipal UserPrincipal principal) {
        return deviceModelAssembler.toModel(deviceService.getDeviceDetails(id, principal.id(), includePrograms));
    }

    @PostMapping
    public ResponseEntity<EntityModel<DeviceResponse>> createDevice(
            @Valid @RequestBody DeviceCreateRequest createRequest,
            @NonNull @AuthenticationPrincipal UserPrincipal principal) {

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
            @AuthenticationPrincipal UserPrincipal principal, Authentication authentication) {

        boolean isAdmin = SecurityUtils.isAdmin(authentication);

        deviceService.deleteDevice(id, principal.id(), isAdmin);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/programs")
    public ResponseEntity<EntityModel<ProgramResponse>> addProgramToDevice(@PathVariable long id,
            @Valid @RequestBody ProgramCreateUpdateRequest createRequest,
            @AuthenticationPrincipal UserPrincipal principal) {

        ProgramResponse program = deviceService.addProgramToDeviceReturningProgram(id, createRequest, principal.id());
        EntityModel<ProgramResponse> model = programModelAssembler.toModel(program);

        return ResponseEntity
                .created(model.getRequiredLink(IanaLinkRelations.SELF).toUri()) // Peke på det nye programmet
                .body(model);
    }

    @PutMapping("/{id}/programs/sync")
    public EntityModel<DeviceResponse> syncPrograms(@PathVariable long id,
            @RequestBody @NotEmpty @Valid List<ProgramSyncRequest> programs,
            @AuthenticationPrincipal UserPrincipal principal) {

        DeviceResponse device = deviceService.syncPrograms(id, programs, principal.id());
        return deviceModelAssembler.toModel(device);
    }

    @GetMapping("/{id}/events")
    public SseEmitter subscribeToEvents(@PathVariable long id, @AuthenticationPrincipal UserPrincipal principal) {
        deviceService.requireOwnedDevice(id, principal.id());

        // Timeout på 0 betyr "uendelig" (eller så lenge serveren tillater)
        SseEmitter emitter = new SseEmitter(0L);
        sseService.addEmitter(id, emitter);

        return emitter;
    }
}
