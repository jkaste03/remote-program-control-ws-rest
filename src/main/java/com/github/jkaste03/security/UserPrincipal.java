package com.github.jkaste03.security;

import java.io.Serializable;
import java.util.Set;

import com.github.jkaste03.model.Role;

public record UserPrincipal(
        Long userid,
        String firstname,
        String lastname,
        String email,
        Set<Role> roles) implements Serializable {
}
