package com.github.jkaste03.controller.api;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.github.jkaste03.model.User;
import com.github.jkaste03.repository.ProgramRepository;
import org.springframework.context.ApplicationEventPublisher;
import com.github.jkaste03.event.WhitelistChangedEvent;
import com.github.jkaste03.exceptions.ConflictException;
import com.github.jkaste03.exceptions.ResourceNotFoundException;
import org.springframework.web.bind.annotation.RestController;

import com.github.jkaste03.dto.UserDto;
import com.github.jkaste03.repository.UserRepository;
import com.github.jkaste03.security.UserPrincipal;

@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

    private static final int MAX_LIMIT = 20;

    private final UserRepository userRepository;
    private final ProgramRepository programRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UserController(UserRepository userRepository, ProgramRepository programRepository,
            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.programRepository = programRepository;
        this.eventPublisher = eventPublisher;
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

    @PutMapping("/me")
    @Transactional
    public UserDto updateUsername(@Valid @RequestBody UsernameUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new ResourceNotFoundException("problem.not-found.user"));
        String username = request.username().trim();
        if (!username.equalsIgnoreCase(user.getUsername())
                && userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("user.conflict.username", username);
        }

        user.setUsername(username);
        userRepository.save(user);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal currentPrincipal) {
            UserPrincipal updatedPrincipal = new UserPrincipal(
                    currentPrincipal.id(),
                    currentPrincipal.email(),
                    username,
                    currentPrincipal.authorities(),
                    currentPrincipal.password());
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            updatedPrincipal,
                            authentication.getCredentials(),
                            updatedPrincipal.getAuthorities()));
        }
        programRepository.findAllByAllowedUsersId(user.getId())
                .forEach(program -> eventPublisher.publishEvent(new WhitelistChangedEvent(this, program)));
        programRepository.findAllByDeviceOwnerId(user.getId())
                .forEach(program -> eventPublisher.publishEvent(new WhitelistChangedEvent(this, program)));
        return new UserDto(user.getId().toString(), user.getEmail(), user.getUsername());
    }

    public record UsernameUpdateRequest(@NotBlank @Size(min = 3, max = 255) String username) {
    }
}
