/**
 * 
 */
package com.github.jkaste03.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;

/**
 * @author tdoy
 */
@Configuration
@EnableMethodSecurity
public class ApplicationSecurity {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http,
			Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter) throws Exception {

		http.csrf(csrf -> csrf.disable());
		http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());
		http.oauth2ResourceServer(
				oauth2 -> oauth2.jwt(jwtconfig -> jwtconfig.jwtAuthenticationConverter(jwtAuthenticationConverter)));

		return http.build();
	}

	@Bean
	public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter(
			UserProvisioningService provisioningService,
			@Value("${keycloak.resource:dat152oblig2}") String clientId) {
		return new UserPrincipalJwtAuthenticationConverter(provisioningService, clientId);
	}
}
