package com.github.jkaste03.security.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import com.github.jkaste03.security.AppRoles;
import com.github.jkaste03.security.UserPrincipal;

@Service
public class KeycloakOidcUserService extends OidcUserService {

    private final String keycloakClientId;

    public KeycloakOidcUserService(
            @org.springframework.beans.factory.annotation.Value("${app.keycloak.client-id}") String keycloakClientId) {
        this.keycloakClientId = keycloakClientId;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        return mapToUserPrincipal(oidcUser);
    }

    private UserPrincipal mapToUserPrincipal(OidcUser oidcUser) {
        String sub = oidcUser.getSubject();
        UUID userId = UUID.fromString(sub); // Antar at Keycloak alltid sender UUID i sub
        String email = oidcUser.getEmail();
        String username = oidcUser.getPreferredUsername();

        Collection<GrantedAuthority> authorities = extractKeycloakClientRoles(oidcUser.getClaims(), keycloakClientId);

        return new UserPrincipal(userId, email, username, authorities, oidcUser.getAttributes(), oidcUser.getIdToken(),
                oidcUser.getUserInfo());
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractKeycloakClientRoles(Map<String, Object> claims, String clientId) {
        Map<String, Object> resourceAccess = (Map<String, Object>) claims.get("resource_access");

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

        return roles.stream()
                .map(roleName -> new SimpleGrantedAuthority(AppRoles.ROLE_PREFIX + roleName))
                .collect(Collectors.toList());
    }
}
