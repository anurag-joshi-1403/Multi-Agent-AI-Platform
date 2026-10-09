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
 * Session-cookie login for the console. Every other {@code /api} route needs the session this
 * starts (see {@link com.project.multi_agent_ai_platform.config.SecurityConfig}).
 * <pre>
 *   POST /api/auth/login    check username + password, start a session   -> 200 {username}
 *   POST /api/auth/logout   end the session (works without one too)      -> 204
 *   GET  /api/auth/me       who the session belongs to                    -> 200 {username} or 401
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

	/** A wrong username or password throws here and becomes the same generic {@code 401} either way. */
	@PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
	public AuthResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request,
			HttpServletResponse response) {
		Authentication result = authenticationManager
			.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));

		// A session id that existed before login must not survive it (session fixation)
		if (request.getSession(false) != null) {
			request.changeSessionId();
		}
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(result);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);

		return new AuthResponse(result.getName());
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
		// Only reachable with a session: SecurityConfig answers 401 before this runs otherwise
		return new AuthResponse(authentication.getName());
	}
}
