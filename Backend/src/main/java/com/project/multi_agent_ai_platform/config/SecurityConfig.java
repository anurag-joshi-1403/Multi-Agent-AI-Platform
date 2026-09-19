package com.project.multi_agent_ai_platform.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Phase 3 security posture: the JSON API and health endpoint are open (the platform runs on a
 * developer machine behind the Vite proxy); everything else — remaining actuator endpoints in
 * particular — stays behind HTTP Basic with Boot's generated password.
 * <p>
 * CSRF is skipped for {@code /api/**} because it is a token-less JSON API consumed by scripts and
 * the SPA, not by browser form posts.
 */
@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
			.cors(Customizer.withDefaults())
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/**", "/actuator/health", "/actuator/health/**", "/actuator/info", "/error")
				.permitAll()
				.anyRequest().authenticated())
			.httpBasic(Customizer.withDefaults());
		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(PlatformProperties properties) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOriginPatterns(properties.cors().allowedOriginPatterns());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", config);
		return source;
	}
}
