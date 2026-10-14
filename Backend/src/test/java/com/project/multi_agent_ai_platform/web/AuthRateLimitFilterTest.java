package com.project.multi_agent_ai_platform.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthRateLimitFilterTest {

	/** A clock the test moves by hand. */
	private static final class FakeClock implements InstantSource {

		Instant now = Instant.parse("2026-10-14T10:00:00Z");

		@Override
		public Instant instant() {
			return now;
		}
	}

	private final FakeClock clock = new FakeClock();

	private final AuthRateLimitFilter filter = new AuthRateLimitFilter(5, clock);

	/** Sends one request through the filter; the chain only runs when the filter lets it pass. */
	private MockHttpServletResponse attempt(String method, String path, String address) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(method, path);
		request.setRemoteAddr(address);
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(request, response, new MockFilterChain());
		return response;
	}

	private MockHttpServletResponse login(String address) throws Exception {
		return attempt("POST", "/api/auth/login", address);
	}

	@Test
	void theSixthAttemptInAMinuteIsRefused() throws Exception {
		for (int i = 0; i < 5; i++) {
			assertThat(login("10.0.0.1").getStatus()).isEqualTo(200);
		}

		MockHttpServletResponse refused = login("10.0.0.1");

		assertThat(refused.getStatus()).isEqualTo(429);
		assertThat(refused.getHeader("Retry-After")).isEqualTo("60");
		assertThat(refused.getContentType()).startsWith("application/problem+json");
		assertThat(refused.getContentAsString()).contains("\"title\":\"Too many attempts\"")
			.contains("Try again in 60 seconds.")
			.contains("\"instance\":\"/api/auth/login\"");
	}

	@Test
	void signUpCountsTooAndRetryAfterShrinks() throws Exception {
		for (int i = 0; i < 5; i++) {
			attempt("POST", "/api/auth/signup", "10.0.0.1");
		}
		clock.now = clock.now.plus(Duration.ofSeconds(45));

		MockHttpServletResponse refused = login("10.0.0.1");

		assertThat(refused.getStatus()).isEqualTo(429);
		assertThat(refused.getHeader("Retry-After")).isEqualTo("15");
	}

	@Test
	void eachAddressHasItsOwnLimit() throws Exception {
		for (int i = 0; i < 6; i++) {
			login("10.0.0.1");
		}

		assertThat(login("10.0.0.2").getStatus()).isEqualTo(200);
	}

	@Test
	void aNewMinuteStartsAfresh() throws Exception {
		for (int i = 0; i < 6; i++) {
			login("10.0.0.1");
		}
		clock.now = clock.now.plus(Duration.ofMinutes(1));

		assertThat(login("10.0.0.1").getStatus()).isEqualTo(200);
	}

	@Test
	void onlyLoginAndSignUpPostsAreCounted() throws Exception {
		for (int i = 0; i < 20; i++) {
			assertThat(attempt("GET", "/api/auth/me", "10.0.0.1").getStatus()).isEqualTo(200);
			assertThat(attempt("POST", "/api/auth/logout", "10.0.0.1").getStatus()).isEqualTo(200);
			assertThat(attempt("POST", "/api/agents/general/run", "10.0.0.1").getStatus()).isEqualTo(200);
		}

		assertThat(login("10.0.0.1").getStatus()).isEqualTo(200);
	}
}
