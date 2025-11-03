package com.github.jkaste03.security;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

import jakarta.servlet.DispatcherType;

@Configuration
@EnableMethodSecurity
public class ApplicationSecurity {

	/**
	 * 1) Egen kjede for H2-konsollen – ingen JWT her.
	 */
	@Bean
	@Order(1) // Viktig: denne må komme før "hovedkjeden"
	public SecurityFilterChain h2SecurityFilterChain(HttpSecurity http) throws Exception {
		http
				.securityMatcher(PathRequest.toH2Console()) // matcher /h2-console/**
				.authorizeHttpRequests(a -> a.anyRequest().permitAll())
				.csrf(csrf -> csrf.disable()) // H2-former takler ikke CSRF godt
				.headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
				// H2-console bruker sesjon/cookies; la det være lov
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.ALWAYS));
		// Ikke konfigurer oauth2ResourceServer i denne kjeden
		return http.build();
	}

	/**
	 * 2) Hovedkjeden – resten av appen krever JWT.
	 */
	@Bean
	@Order(2)
	public SecurityFilterChain appSecurityFilterChain(
			HttpSecurity http,
			Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter) throws Exception {

		http
				// Ekskluder H2 fra hovedkjeden (ikke strengt nødvendig, men ryddig)
				.securityMatcher(new NegatedRequestMatcher(PathRequest.toH2Console()))
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(a -> a
						.dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
						.requestMatchers("/error", "/error/**", "/favicon.ico").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(
						oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

		return http.build();
	}

	@Bean
	public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter(
			UserProvisioningService provisioningService) {
		return new UserPrincipalJwtAuthenticationConverter(provisioningService);
	}
}
