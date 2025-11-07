package com.github.jkaste03.dto.response;

import java.util.List;

public record DeviceResponse(Long id, String name, String userName, List<ProgramResponse> programs) {
}