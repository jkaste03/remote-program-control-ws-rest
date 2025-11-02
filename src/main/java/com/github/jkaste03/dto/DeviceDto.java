package com.github.jkaste03.dto;

import java.util.List;

public record DeviceDto(Long id, String name, String userName, List<ProgramDto> programs) {
}