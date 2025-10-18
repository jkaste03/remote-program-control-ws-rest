/**
 * 
 */
package com.github.jkaste03.security;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * @author tdoy
 */
@Configuration
@EnableMethodSecurity
public class ApplicationSecurity {

	@Autowired
	private AuthTokenFilter authTokenFilter;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http.csrf(csrf -> csrf.disable());
		http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());
		http.oauth2ResourceServer(oauth2 -> oauth2
				.jwt(jwtconfig -> jwtconfig.jwtAuthenticationConverter(jwt -> RoleConverter(jwt))));

		http.addFilterAfter(authTokenFilter, BearerTokenAuthenticationFilter.class);

		return http.build();
	}

	// Spring adds the JwtAuthenticationToken to the SecurityContextHolder
	private JwtAuthenticationToken RoleConverter(Jwt jwt) {

		// initialize
		Collection<GrantedAuthority> rgrantedAuthorities = null;
		Collection<GrantedAuthority> cgrantedAuthorities = null;

		// this is realm roles
		try {
			Map<String, Collection<String>> realmAccess = jwt.getClaim("realm_access");
			Collection<String> realmroles = realmAccess.get("roles");
			rgrantedAuthorities = realmroles.stream()
					.map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
					.collect(Collectors.toList());
		} catch (Exception e) {
			//
		}
		// this is client roles
		try {
			Map<String, Map<String, Collection<String>>> resource_claim = jwt.getClaim("resource_access");
			Map<String, Collection<String>> clientAccess = resource_claim.get("dat152oblig2");
			Collection<String> roles = clientAccess.get("roles");

			cgrantedAuthorities = roles.stream()
					.map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
					.collect(Collectors.toList());
		} catch (Exception e) {
			//
		}

		try {
			cgrantedAuthorities.addAll(rgrantedAuthorities);
		} catch (Exception e) {
			//
		}

		System.out.println("All Roles = " + cgrantedAuthorities);

		return new JwtAuthenticationToken(jwt, cgrantedAuthorities);
	}
}
