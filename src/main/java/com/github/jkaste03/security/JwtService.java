package com.github.jkaste03.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

@Service
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_USERNAME = "preferred_username";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public TokenPair createTokens(UserPrincipal principal) {
        String access = buildToken(principal, props.getAccessTtlSeconds(), TYPE_ACCESS);
        String refresh = buildToken(principal, props.getRefreshTtlSeconds(), TYPE_REFRESH);
        return new TokenPair(access, refresh, props.getAccessTtlSeconds(), props.getRefreshTtlSeconds());
    }

    public String createAccessToken(UserPrincipal principal) {
        return buildToken(principal, props.getAccessTtlSeconds(), TYPE_ACCESS);
    }

    public TokenValidationResult parse(String token) {
        try {
            Jws<Claims> parsed = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(props.getIssuer())
                    .build()
                    .parseSignedClaims(token);

            Claims claims = parsed.getBody();
            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get(CLAIM_EMAIL, String.class);
            String username = claims.get(CLAIM_USERNAME, String.class);
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get(CLAIM_ROLES, List.class);
            String type = claims.get(CLAIM_TYPE, String.class);
            return TokenValidationResult.valid(userId, email, username, roles, type);
        } catch (Exception e) {
            return TokenValidationResult.invalid();
        }
    }

    private String buildToken(UserPrincipal principal, long ttlSeconds, String type) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);

        return Jwts.builder()
                .setIssuer(props.getIssuer())
                .setSubject(principal.id().toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .addClaims(Map.of(
                        CLAIM_EMAIL, principal.email(),
                        CLAIM_USERNAME, principal.username(),
                        CLAIM_ROLES, principal.authorities().stream()
                                .map(a -> a.getAuthority().replace(AppRoles.ROLE_PREFIX, "")) // store without ROLE_
                                .toList(),
                        CLAIM_TYPE, type))
                .signWith(key)
                .compact();
    }

    public record TokenPair(String accessToken, String refreshToken, long expiresInSeconds,
            long refreshExpiresInSeconds) {
    }

    public record TokenValidationResult(boolean valid, UUID userId, String email, String username, List<String> roles,
            String type) {
        public static TokenValidationResult invalid() {
            return new TokenValidationResult(false, null, null, null, List.of(), null);
        }

        public static TokenValidationResult valid(UUID userId, String email, String username, List<String> roles,
                String type) {
            return new TokenValidationResult(true, userId, email, username, roles, type);
        }

        public boolean isAccessToken() {
            return valid && TYPE_ACCESS.equals(type);
        }

        public boolean isRefreshToken() {
            return valid && TYPE_REFRESH.equals(type);
        }
    }
}
