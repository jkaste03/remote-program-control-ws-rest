package com.github.jkaste03.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.github.jkaste03.model.User;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthTokenFilter.class);

	private final UserProvisioningService provisioningService;

	public AuthTokenFilter(UserProvisioningService provisioningService) {
		this.provisioningService = provisioningService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		Authentication current = SecurityContextHolder.getContext().getAuthentication();

		if (current instanceof JwtAuthenticationToken oauthJwtToken) {
			try {
				Jwt jwt = oauthJwtToken.getToken();

				String email = jwt.getClaimAsString("email");
				String first = jwt.getClaimAsString("given_name");
				String last = jwt.getClaimAsString("family_name");

				User user = provisioningService.ensureUserExists(email, safe(first), safe(last));

				// Bygg et lettvekts-principal-objekt (immutabelt)
				UserPrincipal principal = new UserPrincipal(
						user.getUserid(), user.getFirstname(), user.getLastname(), user.getEmail(),
						user.getRoles());

				// Bruk eksisterende authorities fra tokenet (Keycloak roller/scopes)
				UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null,
						oauthJwtToken.getAuthorities());
				auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(auth);
			} catch (Exception e) {
				// Viktig: logg exception riktig
				LOGGER.error("Failed to build authenticated principal from JWT", e);
				// Ikke kast videre (la request gå videre som anonym), med mindre du vil
				// blokkere
			}
		}

		filterChain.doFilter(request, response);
	}

	private static String safe(String s) {
		return s == null ? "" : s;
	}
}
