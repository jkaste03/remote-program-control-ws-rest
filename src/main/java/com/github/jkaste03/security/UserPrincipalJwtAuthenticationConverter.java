package com.github.jkaste03.security;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import com.github.jkaste03.model.User;

import org.springframework.lang.NonNull;

public class UserPrincipalJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter delegate = new JwtGrantedAuthoritiesConverter();
    private final UserProvisioningService provisioningService;

    public UserPrincipalJwtAuthenticationConverter(UserProvisioningService provisioningService) {
        this.provisioningService = Objects.requireNonNull(provisioningService);
    }

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        // Provisionér/oppdater bruker
        String iss = jwt.getIssuer() != null ? jwt.getIssuer().toString() : null;
        String sub = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String username = jwt.getClaimAsString("preferred_username");

        User user = provisioningService.ensureUserFromJwt(iss, sub, username, email);

        var authorities = new LinkedHashSet<GrantedAuthority>();
        var base = delegate.convert(jwt);

        authorities.addAll(base);
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()));

        // Lettvekts principal
        var principal = new UserPrincipal(user.getId(), user.getEmail());

        return new UsernamePasswordAuthenticationToken(principal, jwt, List.copyOf(authorities));
    }
}
