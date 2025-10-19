package com.github.jkaste03.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import com.github.jkaste03.model.User;

/**
 * Builds an {@link UserPrincipal} from the incoming JWT and merges standard
 * authority extraction with Keycloak realm/client roles.
 */
public class UserPrincipalJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserPrincipalJwtAuthenticationConverter.class);

	private final JwtGrantedAuthoritiesConverter delegate = new JwtGrantedAuthoritiesConverter();
	private final UserProvisioningService provisioningService;
	private final String clientId;

	public UserPrincipalJwtAuthenticationConverter(UserProvisioningService provisioningService) {
		this(provisioningService, "dat152oblig2");
	}

	public UserPrincipalJwtAuthenticationConverter(UserProvisioningService provisioningService, String clientId) {
		this.provisioningService = Objects.requireNonNull(provisioningService, "provisioningService");
		this.clientId = Objects.requireNonNull(clientId, "clientId");
	}

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		List<GrantedAuthority> authorities = mergeAuthorities(jwt);

		String email = jwt.getClaimAsString("email");
		String first = safe(jwt.getClaimAsString("given_name"));
		String last = safe(jwt.getClaimAsString("family_name"));

		User user = provisioningService.ensureUserExists(email, first, last);
		UserPrincipal principal = new UserPrincipal(user.getUserid(), user.getFirstname(), user.getLastname(),
				user.getEmail(), user.getRoles());

		return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);
	}

	private List<GrantedAuthority> mergeAuthorities(Jwt jwt) {
		Set<GrantedAuthority> result = new LinkedHashSet<>();
		Collection<GrantedAuthority> base = delegate.convert(jwt);
		if (base != null) {
			result.addAll(base);
		}
		result.addAll(extractRoleAuthorities(jwt));
		return List.copyOf(result);
	}

	@SuppressWarnings("unchecked")
	private Collection<GrantedAuthority> extractRoleAuthorities(Jwt jwt) {
		Set<String> roles = new LinkedHashSet<>();

		Object realmAccess = jwt.getClaim("realm_access");
		if (realmAccess instanceof Map<?, ?> realmMap) {
			Object realmRoles = realmMap.get("roles");
			if (realmRoles instanceof Collection<?> values) {
				roles.addAll(values.stream().map(Object::toString).collect(Collectors.toSet()));
			}
		}

		Object resourceAccess = jwt.getClaim("resource_access");
		if (resourceAccess instanceof Map<?, ?> resourceMap) {
			Object clientAccess = resourceMap.get(clientId);
			if (clientAccess instanceof Map<?, ?> clientMap) {
				Object clientRoles = clientMap.get("roles");
				if (clientRoles instanceof Collection<?> values) {
					roles.addAll(values.stream().map(Object::toString).collect(Collectors.toSet()));
				}
			}
		}

		return roles.stream()
				.filter(role -> role != null && !role.isBlank())
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)))
				.collect(Collectors.toUnmodifiableList());
	}

	private static String safe(String value) {
		return value == null ? "" : value;
	}
}
