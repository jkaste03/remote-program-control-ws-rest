package com.github.jkaste03.service;

import java.util.List;

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
        List<String> allowedUsers = null;
        if (includeAllowedUsers) {
            allowedUsers = program.getAllowedUsers()
                    .stream()
                    .map(User::getUsername)
                    .toList();
        }

        return new ProgramResponse(program.getId(), program.getName(), program.getFilename(), program.isPublic(),
            allowedUsers);
    }
}
