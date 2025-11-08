package com.github.jkaste03.dto.response;

import java.time.Instant;
import java.util.List;

import com.github.jkaste03.model.Device;

public record DeviceResponse(long id, String name, String username, List<ProgramResponse> programs,
        Device.Status status, Instant lastSeen, Instant createdAt, String publicIp, String macAddress) {
    public DeviceResponse(long id, String name, String username, List<ProgramResponse> programs) {
        this(id, name, username, programs, null, null, null, null, null);
    }
}