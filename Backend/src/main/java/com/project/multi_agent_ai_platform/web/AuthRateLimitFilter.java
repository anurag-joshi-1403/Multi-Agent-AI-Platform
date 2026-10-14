package com.project.multi_agent_ai_platform.web;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Limits sign-in and sign-up attempts per client address (5 a minute unless
 * {@code platform.auth.max-attempts-per-minute} says otherwise), so passwords can't be guessed at
 * speed and accounts can't be created in bulk. Every attempt counts, in one-minute windows; over
 * the limit the answer is {@code 429} problem+json with a {@code Retry-After}.
 * <p>
 * The address is the connection's. Behind a reverse proxy, every visitor would share the proxy's,
 * so the proxy's forwarded address must be trusted first ({@code server.forward-headers-strategy}).
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

	private static final Set<String> LIMITED = Set.of("/api/auth/login", "/api/auth/signup");

	private static final Duration WINDOW = Duration.ofMinutes(1);

	/** Past this many addresses, finished windows are dropped so the map can't grow without end. */
	private static final int PRUNE_AT = 10_000;

	private record Window(Instant end, int attempts) {
	}

	private final int maxAttempts;

	private final InstantSource clock;

	private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

	@Autowired
	public AuthRateLimitFilter(@Value("${platform.auth.max-attempts-per-minute:5}") int maxAttempts) {
		this(maxAttempts, InstantSource.system());
	}

	AuthRateLimitFilter(int maxAttempts, InstantSource clock) {
		this.maxAttempts = maxAttempts;
		this.clock = clock;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !"POST".equals(request.getMethod()) || !LIMITED.contains(request.getRequestURI());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Instant now = clock.instant();
		Window window = windows.compute(request.getRemoteAddr(), (address, current) -> current == null
				|| !now.isBefore(current.end()) ? new Window(now.plus(WINDOW), 1)
						: new Window(current.end(), current.attempts() + 1));
		if (windows.size() > PRUNE_AT) {
			windows.values().removeIf(w -> !now.isBefore(w.end()));
		}
		if (window.attempts() > maxAttempts) {
			tooManyAttempts(response, request.getRequestURI(), secondsUntil(now, window.end()));
			return;
		}
		chain.doFilter(request, response);
	}

	private static long secondsUntil(Instant now, Instant end) {
		return Math.max(1, (Duration.between(now, end).toMillis() + 999) / 1000);
	}

	private static void tooManyAttempts(HttpServletResponse response, String path, long retryAfter) throws IOException {
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		// path is one of the two LIMITED constants, so it needs no escaping
		response.getWriter().write("""
				{"type":"https://multi-agent-ai-platform/problems/429","title":"Too many attempts",\
				"status":429,"detail":"Too many attempts from this address. Try again in %d seconds.","instance":"%s"}"""
			.formatted(retryAfter, path));
	}
}
