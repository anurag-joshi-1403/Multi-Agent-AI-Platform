package com.project.multi_agent_ai_platform.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * HTTPS behind a reverse proxy (to-do E3), against a real Tomcat: forwarded headers are handled by
 * the server itself, which MockMvc skips. The tests play the proxy, sending X-Forwarded-For and
 * X-Forwarded-Proto from this machine. MongoDB must be running.
 */
class ForwardedHeadersIntegrationTest {

	private static final HttpClient HTTP = HttpClient.newHttpClient();

	private static HttpResponse<String> post(int port, String path, String json, Map<String, String> headers)
			throws Exception {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(json));
		headers.forEach(request::header);
		return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}

	private static HttpResponse<String> wrongLogin(int port, String visitor) throws Exception {
		return post(port, "/api/auth/login", "{\"username\":\"nobody-here\",\"password\":\"wrong-password\"}",
				Map.of("X-Forwarded-For", visitor));
	}

	/** The session cookie sent back by a successful sign-up. */
	private static String signupCookie(int port, Map<String, String> headers) throws Exception {
		String name = "fwd-" + UUID.randomUUID().toString().substring(0, 8);
		HttpResponse<String> response = post(port, "/api/auth/signup",
				"{\"username\":\"" + name + "\",\"password\":\"a-long-password\"}", headers);
		assertThat(response.statusCode()).isEqualTo(201);
		List<String> cookies = response.headers().allValues("set-cookie");
		return cookies.stream().filter(c -> c.startsWith("JSESSIONID=")).findFirst().orElseThrow();
	}

	@Nested
	@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
			properties = { "server.forward-headers-strategy=native", "platform.auth.max-attempts-per-minute=3" })
	class BehindATrustedProxy {

		@LocalServerPort
		int port;

		@Test
		void eachVisitorGetsTheirOwnRateLimit() throws Exception {
			for (int i = 0; i < 3; i++) {
				assertThat(wrongLogin(port, "203.0.113.5").statusCode()).isEqualTo(401);
			}

			assertThat(wrongLogin(port, "203.0.113.5").statusCode()).isEqualTo(429);
			assertThat(wrongLogin(port, "203.0.113.6").statusCode()).isEqualTo(401);
		}

		@Test
		void theSessionCookieIsSecureWhenTheVisitorCameOverHttps() throws Exception {
			assertThat(signupCookie(port, Map.of("X-Forwarded-Proto", "https", "X-Forwarded-For", "203.0.113.7")))
				.contains("; Secure");
			assertThat(signupCookie(port, Map.of("X-Forwarded-For", "203.0.113.8"))).doesNotContain("Secure");
		}
	}

	@Nested
	@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
			properties = "platform.auth.max-attempts-per-minute=3")
	class WithoutAProxy {

		@LocalServerPort
		int port;

		@Test
		void aFakedAddressDoesNotEscapeTheLimit() throws Exception {
			// Each try claims a different address. Trusted, all five would pass (401); ignored, they
			// all count for this machine, and the limit of 3 cuts in (sooner if the sign-up test ran first).
			List<Integer> statuses = new java.util.ArrayList<>();
			for (int i = 1; i <= 5; i++) {
				statuses.add(wrongLogin(port, "198.51.100." + i).statusCode());
			}

			assertThat(statuses).contains(429);
			assertThat(statuses.getLast()).isEqualTo(429);
		}
	}

	/** Its own server, so the rate-limit test above can't use up this machine's attempts first. */
	@Nested
	@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
	class FakedHttpsWithoutAProxy {

		@LocalServerPort
		int port;

		@Test
		void aFakedHttpsHeaderDoesNotMakeTheCookieSecure() throws Exception {
			assertThat(signupCookie(port, Map.of("X-Forwarded-Proto", "https"))).doesNotContain("Secure");
		}
	}
}
