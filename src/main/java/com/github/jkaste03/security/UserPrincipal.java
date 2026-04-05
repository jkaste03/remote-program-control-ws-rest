package com.github.jkaste03.security;

import java.io.Serializable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record UserPrincipal(
		UUID id,
		String email,
		String username,
		Collection<? extends GrantedAuthority> authorities,
		String password) implements UserDetails, Serializable {

	public UserPrincipal(UUID id, String email, String username) {
		this(id, email, username, Collections.emptyList(), null);
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public String toString() {
		return "UserPrincipal{id=%s, email='%s', username='%s'}".formatted(id, email, username);
	}
}
