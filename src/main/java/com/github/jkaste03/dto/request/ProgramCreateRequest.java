package com.github.jkaste03.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProgramCreateRequest(
        @NotBlank(message = "{name.required}") @Size(max = 100, message = "{name.max-length}") String name,
        @NotNull(message = "{program.is-public.required}") boolean isPublic) {
}
