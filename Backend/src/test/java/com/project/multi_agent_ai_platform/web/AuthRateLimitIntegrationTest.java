package com.project.multi_agent_ai_platform.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** The rate limit wired into the whole application (MongoDB must be running). */
@SpringBootTest(properties = "platform.auth.max-attempts-per-minute=3")
@AutoConfigureMockMvc
class AuthRateLimitIntegrationTest {

	@Autowired
	MockMvc mvc;

	private ResultActions wrongLogin(String address) throws Exception {
		return mvc.perform(post("/api/auth/login").with(request -> {
			request.setRemoteAddr(address);
			return request;
		}).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"nobody-here\",\"password\":\"wrong-password\"}"));
	}

	@Test
	void guessingPasswordsIsCutOffAfterTheLimit() throws Exception {
		for (int i = 0; i < 3; i++) {
			wrongLogin("192.0.2.10").andExpect(status().isUnauthorized());
		}

		wrongLogin("192.0.2.10").andExpect(status().isTooManyRequests())
			.andExpect(header().exists("Retry-After"))
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Too many attempts"));

		// Someone else is not affected
		wrongLogin("192.0.2.11").andExpect(status().isUnauthorized());
	}
}
