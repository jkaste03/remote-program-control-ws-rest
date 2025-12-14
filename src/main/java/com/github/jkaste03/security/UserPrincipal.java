package com.github.jkaste03.security;

import java.io.Serializable;
import java.util.UUID;

public record UserPrincipal(
		UUID id,
		String email,
		String username) implements Serializable {
	@Override
	public String toString() {
		return "UserPrincipal{id=%s, email='%s', username='%s'}".formatted(id, email, username);
	}
}
