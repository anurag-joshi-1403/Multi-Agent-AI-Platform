package com.project.multi_agent_ai_platform.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Console login: {@code POST /api/auth/login} authenticates against the in-memory accounts below
 * and issues a session cookie; every other {@code /api/**} endpoint then requires that session.
 * {@link com.project.multi_agent_ai_platform.web.AuthController} owns the login/logout endpoints
 * themselves — this class only wires the accounts and the access rules.
 * <p>
 * CSRF stays off for {@code /api/**}: it is a token-less JSON API consumed by the SPA and scripts,
 * not by browser form posts, and the session cookie is first-party (same-origin through the Vite
 * dev proxy) rather than carried cross-site.
 */
@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain apiSecurity(HttpSecurity http, SecurityContextRepository securityContextRepository,
			AuthenticationEntryPoint authenticationEntryPoint) throws Exception {
		http
			.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
			.cors(Customizer.withDefaults())
			.securityContext(context -> context.securityContextRepository(securityContextRepository))
			.exceptionHandling(handling -> handling.defaultAuthenticationEntryPointFor(authenticationEntryPoint,
					PathPatternRequestMatcher.pathPattern("/api/**")))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/auth/login", "/actuator/health", "/actuator/health/**", "/actuator/info",
						"/error")
				.permitAll()
				.anyRequest().authenticated())
			.httpBasic(Customizer.withDefaults());
		return http.build();
	}

	/**
	 * A request to {@code /api/**} with no session (or an invalid one) never reaches a controller,
	 * so {@link com.project.multi_agent_ai_platform.web.ApiExceptionHandler} never sees it — this is
	 * what keeps that case on the same RFC 9457 problem+json shape as every other error. Written by
	 * hand rather than through a JSON mapper bean: this project has both Jackson 2 and Jackson 3 on
	 * the classpath (Spring Boot 4's own split), and every value here is a fixed, quote-free literal
	 * or a same-origin request path, so there is nothing for a mapper to protect against.
	 */
	@Bean
	AuthenticationEntryPoint authenticationEntryPoint() {
		return (request, response, authException) -> {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
			response.getWriter().write("""
					{"type":"https://multi-agent-ai-platform/problems/401","title":"Authentication required",\
					"status":401,"detail":"Sign in to use this endpoint.","instance":"%s"}"""
					.formatted(request.getRequestURI()));
		};
	}

	/** Where the authenticated session lives between requests — shared by the filter chain and
	 * {@code AuthController}, which saves into it explicitly after a successful login. */
	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * Console accounts from {@code platform.auth.users} ({@code AUTH_USERS} env var), parsed as
	 * comma-separated {@code username:password} pairs and held in memory — there is no user
	 * database in this platform, matching how documents and conversation memory are also in-memory
	 * today. Passwords are hashed on boot; the plaintext from configuration never persists anywhere.
	 */
	@Bean
	UserDetailsService userDetailsService(PlatformProperties properties, PasswordEncoder encoder) {
		List<UserDetails> users = parseUsers(properties.auth().users()).stream()
			.map(u -> User.withUsername(u.username()).password(encoder.encode(u.password())).roles("USER").build())
			.toList();
		if (users.isEmpty()) {
			throw new IllegalStateException(
					"platform.auth.users (AUTH_USERS) must list at least one username:password pair");
		}
		return new InMemoryUserDetailsManager(users);
	}

	@Bean
	AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder encoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(encoder);
		return new ProviderManager(provider);
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(PlatformProperties properties) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOriginPatterns(properties.cors().allowedOriginPatterns());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		// The session cookie must travel with cross-origin calls too (e.g. VITE_API_BASE pointed
		// straight at the backend instead of through the dev proxy) for login to actually stick.
		config.setAllowCredentials(true);
		config.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", config);
		return source;
	}

	private record Credential(String username, String password) {
	}

	private static List<Credential> parseUsers(String raw) {
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
		return Arrays.stream(raw.split(","))
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.map(SecurityConfig::parseCredential)
			.toList();
	}

	private static Credential parseCredential(String pair) {
		int i = pair.indexOf(':');
		if (i <= 0 || i == pair.length() - 1) {
			throw new IllegalStateException(
					"platform.auth.users entry '" + pair + "' must be in username:password form");
		}
		return new Credential(pair.substring(0, i).trim(), pair.substring(i + 1));
	}
}
