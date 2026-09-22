package com.project.multi_agent_ai_platform.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.project.multi_agent_ai_platform.web.dto.AuthResponse;
import com.project.multi_agent_ai_platform.web.dto.LoginRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * Session-cookie login for the console. Everything else under {@code /api/**} requires the
 * session this issues (see {@link com.project.multi_agent_ai_platform.config.SecurityConfig}).
 * <pre>
 *   POST   /api/auth/login   authenticate, start a session
 *   POST   /api/auth/logout  end the session
 *   GET    /api/auth/me      who the current session belongs to, or 401
 * </pre>
 */
@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

	private final AuthenticationManager authenticationManager;

	private final SecurityContextRepository securityContextRepository;

	public AuthController(AuthenticationManager authenticationManager,
			SecurityContextRepository securityContextRepository) {
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request,
			HttpServletResponse response) {
		Authentication authRequest = UsernamePasswordAuthenticationToken.unauthenticated(body.username(),
				body.password());
		Authentication authResult = authenticationManager.authenticate(authRequest);

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authResult);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);

		return new AuthResponse(authResult.getName());
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		SecurityContextHolder.clearContext();
	}

	@GetMapping("/me")
	public AuthResponse me(Authentication authentication) {
		// SecurityConfig requires authentication for /api/**, so a null/anonymous Authentication
		// here would already have been rejected by the filter chain with a 401 of its own.
		return new AuthResponse(authentication.getName());
	}
}
