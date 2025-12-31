package com.github.jkaste03.controller.api.assembler;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.util.Objects;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.github.jkaste03.controller.api.DeviceController;
import com.github.jkaste03.dto.response.DeviceResponse;
import com.github.jkaste03.security.SecurityUtils;
import com.github.jkaste03.security.UserPrincipal;

@Component
public class DeviceModelAssembler
        implements RepresentationModelAssembler<DeviceResponse, EntityModel<DeviceResponse>> {

    @Override
    @NonNull
    public EntityModel<DeviceResponse> toModel(@NonNull DeviceResponse device) {
        long deviceId = device.id();

        // 1. Hent nåværende autentisering
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 2. Sjekk rettigheter
        boolean isOwner = false;
        boolean isAdmin = SecurityUtils.isAdmin(auth); // Sjekk admin

        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            // Sjekk om innlogget brukers ID matcher device.ownerId()
            isOwner = Objects.equals(principal.id(), device.ownerId());
        }

        // 3. Legg alltid til 'collection'
        EntityModel<DeviceResponse> model = EntityModel.of(device,
                devicesCollectionLink());

        // 4. Legg til administrative linker KUN hvis eier eller admin
        if (isOwner) {
            model.add(deviceSelfLink(deviceId));
            model.add(deviceActionLink(deviceId, "update"));
            model.add(deviceSubResourceLink(deviceId, "programs/sync", "sync-programs"));
            model.add(deviceSubResourceLink(deviceId, "events", "events"));
        }
        if (isAdmin || isOwner) {
            model.add(deviceActionLink(deviceId, "delete"));
        }

        return model;
    }

    @Override
    @NonNull
    public CollectionModel<EntityModel<DeviceResponse>> toCollectionModel(
            @NonNull Iterable<? extends DeviceResponse> devices) {
        CollectionModel<EntityModel<DeviceResponse>> collection = RepresentationModelAssembler.super.toCollectionModel(
                devices);
        collection.add(devicesCollectionLink().withSelfRel());
        collection.add(linkTo(DeviceController.class).withRel("create"));
        return collection;
    }

    @NonNull
    private Link devicesCollectionLink() {
        return linkTo(DeviceController.class)
                .withRel(IanaLinkRelations.COLLECTION.value());
    }

    @NonNull
    private Link deviceSelfLink(long deviceId) {
        return linkTo(DeviceController.class).slash(deviceId).withSelfRel();
    }

    @NonNull
    private Link deviceActionLink(long deviceId, @NonNull String rel) {
        return linkTo(DeviceController.class).slash(deviceId).withRel(rel);
    }

    @NonNull
    private Link deviceSubResourceLink(long deviceId, String path, @NonNull String rel) {
        return linkTo(DeviceController.class)
                .slash(deviceId)
                .slash(path) // Legger til f.eks "programs/sync"
                .withRel(rel);
    }
}
