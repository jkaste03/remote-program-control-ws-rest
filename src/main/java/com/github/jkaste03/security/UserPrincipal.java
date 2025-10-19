package com.github.jkaste03.security;

import java.io.Serializable;

public record UserPrincipal(
		Long id,
		String firstname,
		String lastname,
		String email) implements Serializable {
	@Override
	public String toString() {
		return "UserPrincipal{id=%d, email='%s'}".formatted(id, email);
	}
}
