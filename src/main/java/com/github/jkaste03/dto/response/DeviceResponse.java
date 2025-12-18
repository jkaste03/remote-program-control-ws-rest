package com.github.jkaste03.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.github.jkaste03.model.Device;

public record DeviceResponse(long id, String name, String username, UUID ownerId, List<ProgramResponse> programs,
        Device.Status status, Instant lastSeen, Instant createdAt, String publicIp, Integer wakeOnLanPort,
        String macAddress) {
    public DeviceResponse(long id, String name, String username, UUID ownerId, List<ProgramResponse> programs,
            Device.Status status,
            Instant lastSeen, Instant createdAt) {
        this(id, name, username, ownerId, programs, status, lastSeen, createdAt, null, null, null);
    }
}