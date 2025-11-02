// src/main/java/com/github/jkaste03/security/SecurityUtils.java
package com.github.jkaste03.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static String extractRoleName(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority) // "ROLE_ADMIN"
                .filter(a -> a != null && a.startsWith("ROLE_"))
                .map(a -> a.substring(5)) // "ADMIN"
                .findFirst()
                .orElse(null);
    }
}
