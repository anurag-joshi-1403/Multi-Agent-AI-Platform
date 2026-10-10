package com.project.multi_agent_ai_platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Console login. {@code POST /api/auth/signup} creates an account in the database and
 * {@code POST /api/auth/login} checks one (see {@code AuthController}); both start a session, and
 * every other {@code /api} route then needs that session. Accounts are read by
 * {@code DatabaseUserDetailsService}.
 * <p>
 * CSRF protection is off for {@code /api/**}: it is a JSON API, and the session cookie is
 * {@code SameSite=Strict} (application.properties), so the browser never sends it with a request
 * started by another site.
 */
@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain apiSecurity(HttpSecurity http, SecurityContextRepository securityContextRepository,
			AuthenticationEntryPoint authenticationEntryPoint) throws Exception {
		http
			.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
			.securityContext(context -> context.securityContextRepository(securityContextRepository))
			.exceptionHandling(handling -> handling.authenticationEntryPoint(authenticationEntryPoint))
			// No session for anonymous requests: the request cache would otherwise open one on every 401
			.requestCache(AbstractHttpConfigurer::disable)
			// AuthController owns sign-up, login and logout; Spring's form and /logout endpoints are not used
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/auth/login", "/api/auth/signup", "/api/auth/logout", "/actuator/health", "/error")
				.permitAll()
				.anyRequest()
				.authenticated());
		return http.build();
	}

	/**
	 * A request without a session never reaches a controller, so {@code ApiExceptionHandler} never
	 * sees it. This keeps that {@code 401} on the same problem+json shape as every other error.
	 * Written by hand: every value is a fixed literal except the path, which is escaped.
	 */
	@Bean
	AuthenticationEntryPoint authenticationEntryPoint() {
		return (request, response, authException) -> {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
			response.setCharacterEncoding("UTF-8");
			String path = request.getRequestURI().replace("\\", "\\\\").replace("\"", "\\\"");
			response.getWriter().write("""
					{"type":"https://multi-agent-ai-platform/problems/401","title":"Authentication required",\
					"status":401,"detail":"Sign in to use this endpoint.","instance":"%s"}""".formatted(path));
		};
	}

	/** Where the signed-in session lives; shared with {@code AuthController}, which saves into it on login. */
	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	/** Hashes passwords on sign-up, and checks them on login. */
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/** Checks a login against the accounts {@code userDetailsService} (the database) returns. */
	@Bean
	AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder encoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(encoder);
		return new ProviderManager(provider);
	}
}
