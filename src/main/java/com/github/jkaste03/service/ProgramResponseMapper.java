package com.github.jkaste03.service;

import java.util.List;
import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.github.jkaste03.dto.response.ProgramResponse;
import com.github.jkaste03.model.Program;
import com.github.jkaste03.model.User;

@Component
public class ProgramResponseMapper {

    public @NonNull ProgramResponse toSummary(Program program) {
        return toResponse(program, false);
    }

    public @NonNull ProgramResponse toDetailed(Program program) {
        return toResponse(program, true);
    }

    private @NonNull ProgramResponse toResponse(Program program, boolean includeAllowedUsers) {
        List<String> allowedUsers = List.of();
        List<UUID> allowedUserIds = List.of();
        if (includeAllowedUsers) {
            allowedUsers = program.getAllowedUsers()
                    .stream()
                    .map(User::getUsername)
                    .toList();
            allowedUserIds = program.getAllowedUsers()
                    .stream()
                    .map(User::getId)
                    .toList();
        }

        return new ProgramResponse(
                program.getId(),
                program.getFilename(),
                program.getName(),
                program.isPublic(),
                program.getStatus(),
                program.getDevice().getOwner().getId(),
                allowedUsers,
                allowedUserIds);
    }
}
