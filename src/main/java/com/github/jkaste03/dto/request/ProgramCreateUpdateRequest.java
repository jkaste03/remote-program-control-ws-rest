package com.github.jkaste03.dto.request;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProgramCreateUpdateRequest(
        @NotBlank(message = "{filename.required}") @Size(max = 255, message = "{filename.max-length}") String filename,
        @NotBlank(message = "{name.required}") @Size(max = 255, message = "{name.max-length}") String name,
        @NotNull(message = "{program.is-public.required}") boolean isPublic,
        Set<UUID> allowedUserIds) {
    // Kompakt konstruktør: Kjøres hver gang recorden opprettes
    public ProgramCreateUpdateRequest {
        if (allowedUserIds == null) {
            allowedUserIds = Collections.emptySet();
        }
        // Vi kan også gjøre Set-et "immutable" for sikkerhets skyld
        allowedUserIds = Collections.unmodifiableSet(allowedUserIds);
    }
}