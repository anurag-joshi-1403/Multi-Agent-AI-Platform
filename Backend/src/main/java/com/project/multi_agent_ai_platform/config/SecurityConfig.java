package com.project.multi_agent_ai_platform.config;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Console login. {@code POST /api/auth/login} checks the accounts below and starts a session (see
 * {@code AuthController}); every other {@code /api} route then needs that session.
 * <p>
 * CSRF protection is off for {@code /api/**}: it is a JSON API, and the session cookie is
 * {@code SameSite=Strict} (application.properties), so the browser never sends it with a request
 * started by another site.
 */
@Configuration
public class SecurityConfig {

	private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

	@Bean
	SecurityFilterChain apiSecurity(HttpSecurity http, SecurityContextRepository securityContextRepository,
			AuthenticationEntryPoint authenticationEntryPoint) throws Exception {
		http
			.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
			.securityContext(context -> context.securityContextRepository(securityContextRepository))
			.exceptionHandling(handling -> handling.authenticationEntryPoint(authenticationEntryPoint))
			// No session for anonymous requests: the request cache would otherwise open one on every 401
			.requestCache(AbstractHttpConfigurer::disable)
			// AuthController owns login and logout; Spring's form and /logout endpoints are not used
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/auth/login", "/api/auth/logout", "/actuator/health", "/error")
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

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * Accounts from {@code platform.auth.users} ({@code AUTH_USERS}), hashed on boot and held in
	 * memory; the plain passwords are never stored. With none configured, a single {@code admin}
	 * account gets a random password, printed once in the log, so the app is never open with a
	 * guessable default.
	 */
	@Bean
	UserDetailsService userDetailsService(PlatformProperties properties, PasswordEncoder encoder) {
		List<Credential> credentials = parseUsers(properties.auth().users());
		if (credentials.isEmpty()) {
			String password = randomPassword();
			log.warn("AUTH_USERS is not set. Sign in as 'admin' with this one-time password: {} "
					+ "(it changes on every restart; set AUTH_USERS=you:your-password in Backend/.env to fix it)",
					password);
			credentials = List.of(new Credential("admin", password));
		}
		List<UserDetails> users = credentials.stream()
			.map(c -> User.withUsername(c.username()).password(encoder.encode(c.password())).roles("USER").build())
			.toList();
		return new InMemoryUserDetailsManager(users);
	}

	@Bean
	AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder encoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(encoder);
		return new ProviderManager(provider);
	}

	record Credential(String username, String password) {
	}

	/**
	 * {@code "anna:s3cret, ben:pa:ss"} → two accounts. The first {@code :} splits each pair, so a
	 * password may contain {@code :} but not {@code ,}. A malformed or repeated entry stops startup
	 * rather than silently dropping an account.
	 */
	static List<Credential> parseUsers(String raw) {
		List<Credential> credentials = new ArrayList<>();
		if (raw == null || raw.isBlank()) {
			return credentials;
		}
		Set<String> seen = new HashSet<>();
		for (String entry : raw.split(",")) {
			String pair = entry.trim();
			if (pair.isEmpty()) {
				continue;
			}
			int colon = pair.indexOf(':');
			if (colon <= 0 || colon == pair.length() - 1) {
				throw new IllegalStateException("AUTH_USERS entry '" + pair.substring(0, Math.max(colon, 0))
						+ "…' must be in username:password form");
			}
			String username = pair.substring(0, colon).trim();
			if (!seen.add(username)) {
				throw new IllegalStateException("AUTH_USERS lists '" + username + "' more than once");
			}
			credentials.add(new Credential(username, pair.substring(colon + 1)));
		}
		return credentials;
	}

	private static String randomPassword() {
		byte[] bytes = new byte[12];
		new SecureRandom().nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
