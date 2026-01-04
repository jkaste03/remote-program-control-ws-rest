package com.github.jkaste03.dto.response;

import java.time.Instant;
import java.util.UUID;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;

import com.github.jkaste03.model.Device;

/**
 * HAL-friendly device representation that embeds programs with HATEOAS-links.
 */
public class DeviceResource extends RepresentationModel<DeviceResource> {

    private final long id;
    private final String name;
    private final String username;
    private final UUID ownerId;
    private final Device.Status status;
    private final Instant lastSeen;
    private final Instant createdAt;
    private final String publicIp;
    private final Integer wakeOnLanPort;
    private final String macAddress;
    private final CollectionModel<EntityModel<ProgramResponse>> programs;

    public DeviceResource(long id, String name, String username, UUID ownerId,
            Device.Status status, Instant lastSeen, Instant createdAt, String publicIp,
            Integer wakeOnLanPort, String macAddress,
            CollectionModel<EntityModel<ProgramResponse>> programs) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.ownerId = ownerId;
        this.status = status;
        this.lastSeen = lastSeen;
        this.createdAt = createdAt;
        this.publicIp = publicIp;
        this.wakeOnLanPort = wakeOnLanPort;
        this.macAddress = macAddress;
        this.programs = programs;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Device.Status getStatus() {
        return status;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public Integer getWakeOnLanPort() {
        return wakeOnLanPort;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public CollectionModel<EntityModel<ProgramResponse>> getPrograms() {
        return programs;
    }
}