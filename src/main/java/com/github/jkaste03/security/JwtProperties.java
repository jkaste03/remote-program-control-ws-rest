package com.github.jkaste03.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    /**
     * HS256 secret key. Must be at least 32 bytes.
     */
    private String secret = "change-me-dev-secret-change-me-dev-secret";

    /**
     * Issuer claim.
     */
    private String issuer = "remote-program-control";

    /**
     * Access token lifetime in seconds.
     */
    private long accessTtlSeconds = 15 * 60; // 15 minutes

    /**
     * Refresh token lifetime in seconds.
     */
    private long refreshTtlSeconds = 7 * 24 * 60 * 60; // 7 days

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public long getAccessTtlSeconds() {
        return accessTtlSeconds;
    }

    public void setAccessTtlSeconds(long accessTtlSeconds) {
        this.accessTtlSeconds = accessTtlSeconds;
    }

    public long getRefreshTtlSeconds() {
        return refreshTtlSeconds;
    }

    public void setRefreshTtlSeconds(long refreshTtlSeconds) {
        this.refreshTtlSeconds = refreshTtlSeconds;
    }
}
