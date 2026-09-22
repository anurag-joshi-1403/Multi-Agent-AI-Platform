package com.project.multi_agent_ai_platform.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.project.multi_agent_ai_platform.config.LlmProvider;
import com.project.multi_agent_ai_platform.config.PlatformProperties;
import com.project.multi_agent_ai_platform.config.SecurityConfig;

/**
 * Exercises the real session flow end to end (no {@code @WithMockUser} or default-authenticated
 * MockMvc shortcuts here — this is the one test class whose job is to prove login itself actually
 * works): login sets a session cookie, that cookie authenticates the next request, wrong
 * credentials are rejected without revealing which part was wrong, and logout ends the session.
 */
@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, AuthControllerTest.ProviderConfig.class })
@EnableConfigurationProperties(PlatformProperties.class)
class AuthControllerTest {

	@TestConfiguration
	static class ProviderConfig {

		// ApiExceptionHandler (auto-picked-up by @WebMvcTest as part of the web layer) needs this,
		// even though no test here exercises a provider-error path.
		@Bean
		LlmProvider llmProvider() {
			return LlmProvider.GEMINI;
		}
	}

	@Autowired
	MockMvc mvc;

	private static final String LOGIN_BODY = "{\"username\":\"admin\",\"password\":\"admin\"}";

	@Test
	void loginSucceedsAndTheSessionAuthenticatesTheNextRequest() throws Exception {
		var loginResult = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("admin"))
			.andReturn();

		mvc.perform(get("/api/auth/me").session((org.springframework.mock.web.MockHttpSession) loginResult.getRequest()
				.getSession()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("admin"));
	}

	@Test
	void wrongPasswordIs401WithoutRevealingWhichFieldWasWrong() throws Exception {
		mvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"admin\",\"password\":\"nope\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.title").value("Invalid credentials"));
	}

	@Test
	void unknownUsernameIs401WithTheSameMessageAsAWrongPassword() throws Exception {
		mvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"nobody\",\"password\":\"admin\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.title").value("Invalid credentials"));
	}

	@Test
	void blankCredentialsAreRejectedBeforeAuthenticating() throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"\",\"password\":\"\"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void meWithoutASessionIs401() throws Exception {
		mvc.perform(get("/api/auth/me").with(anonymous())).andExpect(status().isUnauthorized());
	}

	@Test
	void logoutEndsTheSession() throws Exception {
		var loginResult = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
			.andExpect(status().isOk())
			.andReturn();
		var session = (org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession();

		mvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isNoContent());

		mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
	}
}
