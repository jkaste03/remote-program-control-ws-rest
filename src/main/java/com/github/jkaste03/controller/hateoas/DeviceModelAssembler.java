package com.github.jkaste03.controller.hateoas;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.github.jkaste03.controller.DeviceController;
import com.github.jkaste03.dto.response.DeviceResponse;

@Component
public class DeviceModelAssembler
        implements RepresentationModelAssembler<DeviceResponse, EntityModel<DeviceResponse>> {

    @Override
    @NonNull
    public EntityModel<DeviceResponse> toModel(@NonNull DeviceResponse device) {
        long deviceId = device.id();
        return EntityModel.of(device,
                deviceSelfLink(deviceId),
                deviceActionLink(deviceId, "update"),
                deviceActionLink(deviceId, "delete"),
                devicesCollectionLink());
    }

    @Override
    @NonNull
    public CollectionModel<EntityModel<DeviceResponse>> toCollectionModel(
            @NonNull Iterable<? extends DeviceResponse> devices) {
        CollectionModel<EntityModel<DeviceResponse>> collection = RepresentationModelAssembler.super.toCollectionModel(
                devices);
        collection.add(devicesCollectionLink());
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
}
