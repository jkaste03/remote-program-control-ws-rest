package com.github.jkaste03.controller.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.jkaste03.security.JwtService;
import com.github.jkaste03.security.JwtService.TokenPair;
import com.github.jkaste03.security.LocalUserDetailsService;
import com.github.jkaste03.security.AppRoles;
import com.github.jkaste03.security.UserPrincipal;
import com.github.jkaste03.repository.UserRepository;
import com.github.jkaste03.repository.RoleRepository;
import com.github.jkaste03.model.Role;
import com.github.jkaste03.model.User;
import com.github.jkaste03.exceptions.ConflictException;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LocalUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
            LocalUserDetailsService userDetailsService,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserPrincipal principal = toPrincipal(authentication.getPrincipal());
        TokenPair pair = jwtService.createTokens(principal);

        return ResponseEntity.ok(new TokenResponse(pair.accessToken(), pair.refreshToken(),
                pair.expiresInSeconds(), pair.refreshExpiresInSeconds(), principal.username(), principal.id().toString()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        var parsed = jwtService.parse(request.refreshToken());
        if (!parsed.valid() || !parsed.isRefreshToken()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserDetails user = userDetailsService.loadUserById(parsed.userId());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserPrincipal principal = toPrincipal(user);
        TokenPair pair = jwtService.createTokens(principal);

        return ResponseEntity.ok(new TokenResponse(pair.accessToken(), pair.refreshToken(),
                pair.expiresInSeconds(), pair.refreshExpiresInSeconds(), principal.username(), principal.id().toString()));
    }

    @PostMapping("/me")
    public ResponseEntity<UserInfoResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new UserInfoResponse(principal.id().toString(), principal.username(),
                principal.email()));
    }

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("user.conflict.email", request.email());
        }
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new ConflictException("user.conflict.username", request.username());
        }

        Role role = roleRepository.findByName(AppRoles.USER)
                .orElseThrow(() -> new IllegalStateException("Required role USER not found"));

        User user = new User(UUID.randomUUID(), request.email(), request.username(), role,
                passwordEncoder.encode(request.password()));
        userRepository.save(user);

        var authority = new SimpleGrantedAuthority(AppRoles.ROLE_PREFIX + role.getName());
        UserPrincipal principal = new UserPrincipal(user.getId(), user.getEmail(), user.getUsername(),
                java.util.List.of(authority), user.getPassword());
        TokenPair pair = jwtService.createTokens(principal);

        return ResponseEntity.status(HttpStatus.CREATED).body(new TokenResponse(pair.accessToken(), pair.refreshToken(),
                pair.expiresInSeconds(), pair.refreshExpiresInSeconds(), principal.username(),
                principal.id().toString()));
    }

    /* DTOs */
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 3, max = 255) String username,
            @NotBlank @Size(min = 8, max = 255) String password) {
    }

    public record TokenResponse(String accessToken, String refreshToken, long expiresIn, long refreshExpiresIn,
            String username, String userId) {
    }

    public record UserInfoResponse(String id, String username, String email) {
    }

    private UserPrincipal toPrincipal(Object principalObj) {
        if (principalObj instanceof UserPrincipal up) {
            return up;
        }
        if (principalObj instanceof UserDetails ud) {
            return new UserPrincipal(null, ud.getUsername(), ud.getUsername(), ud.getAuthorities(), ud.getPassword());
        }
        throw new IllegalStateException("Unexpected principal type: " + principalObj);
    }
}
