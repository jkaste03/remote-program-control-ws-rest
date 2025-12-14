package com.github.jkaste03.security;

public final class AppRoles {

    // Grunnleggende rollenavn (slik de ligger i databasen/Keycloak)
    public static final String ADMIN = "ADMIN";
    public static final String USER = "USER";

    // Prefix som Spring Security krever
    public static final String ROLE_PREFIX = "ROLE_";

    // Ferdige rettighetsnavn for bruk i @PreAuthorize og sjekker
    public static final String ROLE_ADMIN = ROLE_PREFIX + ADMIN;
    public static final String ROLE_USER = ROLE_PREFIX + USER;

    private AppRoles() {
        // Hindre instansiering
    }
}