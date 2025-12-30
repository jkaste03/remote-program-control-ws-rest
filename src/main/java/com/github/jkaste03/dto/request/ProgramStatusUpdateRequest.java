package com.github.jkaste03.dto.request;

import com.github.jkaste03.model.Program.Status;

import jakarta.validation.constraints.NotNull;

public record ProgramStatusUpdateRequest(
        @NotNull(message = "{status.required}") Status status) {
}
