package com.github.jkaste03.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProgramSyncRequest(
        @NotBlank(message = "{filename.required}") @Size(max = 255, message = "{filename.max-length}") String filename) {
}
