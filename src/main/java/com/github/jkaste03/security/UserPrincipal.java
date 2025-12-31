package com.github.jkaste03.security;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public record UserPrincipal(
		UUID id,
		String email,
		String username,
		Collection<? extends GrantedAuthority> authorities,
		Map<String, Object> attributes,
		OidcIdToken idToken,
		OidcUserInfo userInfo) implements OidcUser, Serializable {

	// Forenklet konstruktør for API (JWT) bruk
	public UserPrincipal(UUID id, String email, String username) {
		this(id, email, username, Collections.emptyList(), Collections.emptyMap(), null, null);
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public Map<String, Object> getClaims() {
		return attributes;
	}

	@Override
	public OidcUserInfo getUserInfo() {
		return userInfo;
	}

	@Override
	public OidcIdToken getIdToken() {
		return idToken;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getName() {
		return username;
	}

	@Override
	public String toString() {
		return "UserPrincipal{id=%s, email='%s', username='%s'}".formatted(id, email, username);
	}
}
