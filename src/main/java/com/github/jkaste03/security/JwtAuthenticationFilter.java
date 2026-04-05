package com.github.jkaste03.security;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final LocalUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, LocalUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = null;

        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            token = header.substring(7);
        } else {
            // Allow token via query param for SSE/EventSource clients that cannot set headers
            String paramToken = request.getParameter("access_token");
            if (StringUtils.hasText(paramToken)) {
                token = paramToken;
            }
        }

        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        JwtService.TokenValidationResult result = jwtService.parse(token);

        if (!result.valid() || !result.isAccessToken()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Avoid re-authentication if context already set
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            UserDetails userDetails = userDetailsService.loadUserById(result.userId());
            if (userDetails == null) {
                log.warn("User id {} from token not found", result.userId());
                filterChain.doFilter(request, response);
                return;
            }

            Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
            // If DB lacks roles, fallback to token roles
            if (authorities == null || authorities.isEmpty()) {
                authorities = result.roles().stream()
                        .map(role -> new SimpleGrantedAuthority(AppRoles.ROLE_PREFIX + role))
                        .collect(Collectors.toList());
            }

            var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.debug("JWT auth failed: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
