// com/github/jkaste03/security/UserPrincipalJwtAuthenticationConverter.java
package com.github.jkaste03.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

public class UserPrincipalJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter delegate = new JwtGrantedAuthoritiesConverter();
    private final UserProvisioningService provisioningService;
    private final String clientId;

    public UserPrincipalJwtAuthenticationConverter(UserProvisioningService provisioningService, String clientId) {
        this.provisioningService = Objects.requireNonNull(provisioningService);
        this.clientId = Objects.requireNonNull(clientId);
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // Provisionér/oppdater bruker
        String email = jwt.getClaimAsString("email");
        String first = orEmpty(jwt.getClaimAsString("given_name"));
        String last = orEmpty(jwt.getClaimAsString("family_name"));
        var user = provisioningService.ensureUserExists(email, first, last);

        // Authorities: base (scopes) + Keycloak-roller
        Set<String> roleNames = extractRoleNames(jwt, clientId);
        var authorities = new LinkedHashSet<GrantedAuthority>();
        var base = delegate.convert(jwt);

        authorities.addAll(base);
        roleNames.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));

        // Lettvekts principal
        var principal = new UserPrincipal(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail());

        return new UsernamePasswordAuthenticationToken(principal, jwt, List.copyOf(authorities));
    }

    private static String orEmpty(String v) {
        return v == null ? "" : v;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> extractRoleNames(Jwt jwt, String clientId) {
        var roles = new LinkedHashSet<String>();

        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof Map<?, ?> m && m.get("roles") instanceof Collection<?> vals) {
            vals.forEach(v -> {
                if (v != null)
                    roles.add(v.toString());
            });
        }

        Object resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess instanceof Map<?, ?> m && m.get(clientId) instanceof Map<?, ?> cm) {
            Object clientRoles = cm.get("roles");
            if (clientRoles instanceof Collection<?> vals) {
                vals.forEach(v -> {
                    if (v != null)
                        roles.add(v.toString());
                });
            }
        }

        return roles.stream()
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
