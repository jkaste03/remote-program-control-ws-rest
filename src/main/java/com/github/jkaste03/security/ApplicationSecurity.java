package com.github.jkaste03.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.github.jkaste03.security.web.KeycloakOidcUserService;

import jakarta.servlet.DispatcherType;

@Configuration
@EnableMethodSecurity
public class ApplicationSecurity {

	// Inject verdien fra application.properties
	@Value("${app.keycloak.client-id}")
	private String keycloakClientId;

	// H2-console er kun aktivert i dev, aldri i prod
	@Value("${spring.h2.console.enabled:false}")
	private boolean h2ConsoleEnabled;

	private final KeycloakOidcUserService keycloakOidcUserService;

	public ApplicationSecurity(KeycloakOidcUserService keycloakOidcUserService) {
		this.keycloakOidcUserService = keycloakOidcUserService;
	}

	/**
	 * 1) Egen kjede for H2-konsollen – ingen JWT her.
	 */
	@Bean
	@Order(1) // Viktig: denne må komme før "hovedkjeden"
	@ConditionalOnProperty(value = "spring.h2.console.enabled", havingValue = "true")
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
			Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter,
			ClientRegistrationRepository clientRegistrationRepository) throws Exception {

		// I prod er H2-konsollen av, så ikke bruk PathRequest (mangler bean)
		if (h2ConsoleEnabled) {
			RequestMatcher nonH2Matcher = new NegatedRequestMatcher(PathRequest.toH2Console());
			http.securityMatcher(nonH2Matcher);
		}

		http
				.csrf(csrf -> csrf.disable())
				// Vi tillater session for OAuth2 login flyten
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.authorizeHttpRequests(a -> a
						.dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
						.requestMatchers("/error", "/error/**", "/favicon.ico").permitAll()
						.anyRequest().authenticated())
				.oauth2Login(oauth2 -> oauth2
						.userInfoEndpoint(userInfo -> userInfo.oidcUserService(keycloakOidcUserService)))
				.logout(logout -> logout
						.logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
						.logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository))
						.invalidateHttpSession(true)
						.clearAuthentication(true)
						.deleteCookies("JSESSIONID")
						.permitAll())
				.oauth2ResourceServer(
						oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

		return http.build();
	}

	@Bean
	public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter(
			UserProvisioningService userProvisioningService) {
		return new UserPrincipalJwtAuthenticationConverter(keycloakClientId, userProvisioningService);
	}

	private LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository) {
		OidcClientInitiatedLogoutSuccessHandler handler = new OidcClientInitiatedLogoutSuccessHandler(
				clientRegistrationRepository);
		handler.setPostLogoutRedirectUri("{baseUrl}/");
		return handler;
	}
}
