package com.github.jkaste03.dto.response;

import java.util.List;
import java.util.UUID;

import com.github.jkaste03.model.Program.Status;

public record ProgramResponse(long id, String filename, String name, boolean isPublic, Status status,
        UUID ownerId, List<String> allowedUsers) {
}