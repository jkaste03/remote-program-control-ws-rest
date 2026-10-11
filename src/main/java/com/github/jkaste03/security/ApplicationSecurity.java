package com.github.jkaste03.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.core.annotation.Order;

import jakarta.servlet.DispatcherType;

@Configuration
@EnableMethodSecurity
public class ApplicationSecurity {

	private final LocalUserDetailsService userDetailsService;
	private final JwtService jwtService;
	private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

	public ApplicationSecurity(LocalUserDetailsService userDetailsService,
			JwtService jwtService,
			RestAuthenticationEntryPoint restAuthenticationEntryPoint) {
		this.userDetailsService = userDetailsService;
		this.jwtService = jwtService;
		this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder());
		return provider;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	@ConditionalOnProperty(value = "spring.h2.console.enabled", havingValue = "true")
	@Order(0)
	public SecurityFilterChain h2SecurityFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher(PathRequest.toH2Console())
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
				.csrf(csrf -> csrf.disable())
				.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.ALWAYS));
		return http.build();
	}

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        http.securityMatcher(new OrRequestMatcher(
                new AntPathRequestMatcher("/remote-program-control/api/**"),
                new AntPathRequestMatcher("/api/**")))
                .csrf(csrf -> csrf.disable())
                // Browser requests share the form-login session, while the agent can still
                // authenticate with a Bearer JWT through JwtAuthenticationFilter.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        http.authenticationProvider(authenticationProvider());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain mainSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/auth/**"))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, exception) ->
                        response.sendRedirect(request.getContextPath() + "/login")))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/.well-known/**").permitAll()
                        .requestMatchers("/auth/**", "/login", "/register", "/error", "/favicon.ico",
                                "/css/**", "/js/**", "/i18n/**", "/images/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(302);
                            response.setHeader("Location", request.getContextPath() + "/login?error");
                        })
                        .successHandler((request, response, authentication) -> {
                            String ctx = request.getContextPath();
                            if (ctx == null) ctx = "";
                            if (!ctx.endsWith("/")) ctx += "/";
                            response.setStatus(302);
                            response.setHeader("Location", ctx);
                        })
                        .permitAll())
                .logout(logout -> logout.logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"));

        http.authenticationProvider(authenticationProvider());
        return http.build();
    }
}
