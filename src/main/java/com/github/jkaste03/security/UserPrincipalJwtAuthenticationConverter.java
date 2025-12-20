package com.github.jkaste03.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import com.github.jkaste03.exceptions.InvalidTokenException;
import com.github.jkaste03.service.SseService;

import org.springframework.lang.NonNull;

public class UserPrincipalJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger log = LoggerFactory.getLogger(SseService.class);
    private static final String INVALID_TOKEN_MESSAGE_KEY = "problem.auth.invalid-token";

    private static final String EMAIL_CLAIM = "email";
    private static final String USERNAME_CLAIM = "preferred_username";

    // Navnet på klienten i Keycloak som inneholder rollene (fra JSON-en din)
    private final String keycloakClientId;

    public UserPrincipalJwtAuthenticationConverter(String keycloakClientId) {
        this.keycloakClientId = keycloakClientId;
    }

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        String sub = jwt.getSubject();
        UUID userId;
        try {
            userId = UUID.fromString(sub);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID in 'sub' claim: {}", sub);
            throw new InvalidTokenException(INVALID_TOKEN_MESSAGE_KEY, e);
        }

        String email = jwt.getClaimAsString(EMAIL_CLAIM);
        if (email == null) {
            log.warn("JWT missing required claim: {}", EMAIL_CLAIM);
            throw new InvalidTokenException(INVALID_TOKEN_MESSAGE_KEY);
        }

        String username = jwt.getClaimAsString(USERNAME_CLAIM);
        if (username == null) {
            log.warn("JWT missing required claim: {}", USERNAME_CLAIM);
            throw new InvalidTokenException(INVALID_TOKEN_MESSAGE_KEY);
        }

        // 2. Hent roller fra Keycloak sin spesifikke struktur
        Collection<GrantedAuthority> authorities = extractKeycloakClientRoles(jwt, keycloakClientId);

        // 3. Lag UserPrincipal (lettvekts, ingen DB-kall)
        var principal = new UserPrincipal(userId, email, username);

        return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractKeycloakClientRoles(Jwt jwt, String clientId) {
        // I Keycloak JSON ligger dette under "clientRoles", men i selve Access Tokenet
        // (JWT) ligger det vanligvis under strukturen: resource_access -> klient-navn
        // -> roles
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");

        if (resourceAccess == null || resourceAccess.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(clientId);
        if (clientAccess == null || clientAccess.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> roles = (List<String>) clientAccess.get("roles");
        if (roles == null) {
            return new ArrayList<>();
        }

        // Mapper om fra "ADMIN" til "ROLE_ADMIN" som Spring Security liker
        return roles.stream()
                .map(roleName -> new SimpleGrantedAuthority(AppRoles.ROLE_PREFIX + roleName))
                .collect(Collectors.toList());
    }
}
