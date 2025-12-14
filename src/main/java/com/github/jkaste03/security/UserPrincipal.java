package com.github.jkaste03.security;

import java.io.Serializable;

public record UserPrincipal(
		String id,
		String email,
		String username) implements Serializable {
	@Override
	public String toString() {
		return "UserPrincipal{id=%s, email='%s', username='%s'}".formatted(id, email, username);
	}
}
