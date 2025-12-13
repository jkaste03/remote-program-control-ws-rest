package com.github.jkaste03.dto.response;

import java.util.List;

public record ProgramResponse(long id, String filename, String name, boolean isPublic, List<String> allowedUsers) {
}