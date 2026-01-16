package com.github.jkaste03.controller.api;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.jkaste03.dto.UserDto;
import com.github.jkaste03.repository.UserRepository;
import com.github.jkaste03.security.UserPrincipal;

@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping(value = "/remote-program-control/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

    private static final int MAX_LIMIT = 20;

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/search")
    public List<UserDto> searchUsers(
            @RequestParam("q") String query,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (query == null || query.isBlank()) {
            return List.of();
        }

        String trimmed = query.trim();
        int pageSize = Math.max(1, Math.min(limit, MAX_LIMIT));
        var currentUserId = principal != null ? principal.id() : null;

        return userRepository
                .findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(trimmed, trimmed,
                        PageRequest.of(0, pageSize))
                .filter(u -> currentUserId == null || !u.getId().equals(currentUserId))
                .map(u -> new UserDto(u.getId().toString(), u.getEmail(), u.getUsername()))
                .toList();
    }
}
