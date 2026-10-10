package com.project.multi_agent_ai_platform.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.project.multi_agent_ai_platform.user.UserAccount;
import com.project.multi_agent_ai_platform.user.UserStore;

/**
 * The real sign-up and login flow against the whole application, with no "signed in by default"
 * shortcut: the one place that proves the session itself works and that nothing under {@code /api}
 * is open. Accounts live in MongoDB ({@code agents_test} database), so MongoDB must be running.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	UserStore users;

	@Autowired
	PasswordEncoder encoder;

	/** The account the login tests sign in with, created once in the test database. */
	@BeforeEach
	void testerExists() {
		if (users.findByUsername("tester").isEmpty()) {
			users.create("tester", encoder.encode("tester-password"));
		}
	}

	private ResultActions login(String username, String password) throws Exception {
		return mvc.perform(post("/api/auth/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
	}

	private ResultActions signup(String username, String password) throws Exception {
		return mvc.perform(post("/api/auth/signup")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
	}

	/** A name no earlier run used, since the test database keeps its accounts between runs. */
	private static String freshName() {
		return "new-" + UUID.randomUUID().toString().substring(0, 8);
	}

	private MockHttpSession signedInSession() throws Exception {
		MvcResult result = login("tester", "tester-password").andExpect(status().isOk()).andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	@ParameterizedTest(name = "{0} {1}")
	@CsvSource({ "GET, /api/agents", "GET, /api/agents/general", "POST, /api/agents/general/run",
			"GET, /api/platform", "POST, /api/documents", "DELETE, /api/documents/doc_x",
			"DELETE, /api/conversations/c-1", "GET, /api/auth/me" })
	void everyApiRouteNeedsASession(String method, String path) throws Exception {
		MvcResult result = mvc.perform(request(HttpMethod.valueOf(method), path))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Authentication required"))
			.andExpect(jsonPath("$.detail").value("Sign in to use this endpoint."))
			.andExpect(jsonPath("$.instance").value(path))
			.andReturn();
		// a rejected request must not open a session
		assertThat(result.getRequest().getSession(false)).isNull();
	}

	@Test
	void healthStaysOpenForTheProxyAndMonitoring() throws Exception {
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void signInUseTheApiThenSignOut() throws Exception {
		MockHttpSession session = signedInSession();

		mvc.perform(get("/api/auth/me").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("tester"));
		mvc.perform(get("/api/agents").session(session)).andExpect(status().isOk());

		mvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isNoContent());

		mvc.perform(get("/api/agents").session(session)).andExpect(status().isUnauthorized());
	}

	@Test
	void signingInReturnsTheUsername() throws Exception {
		login("tester", "tester-password").andExpect(jsonPath("$.username").value("tester"));
	}

	@Test
	void wrongPasswordAndUnknownUserGetTheSameAnswer() throws Exception {
		for (String[] attempt : new String[][] { { "tester", "wrong" }, { "nobody", "tester-password" } }) {
			MvcResult result = login(attempt[0], attempt[1])
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Invalid credentials"))
				.andExpect(jsonPath("$.detail").value("Incorrect username or password."))
				.andReturn();
			assertThat(result.getRequest().getSession(false)).isNull();
		}
	}

	@Test
	void blankCredentialsAre400BeforeAnyCheck() throws Exception {
		login("", "").andExpect(status().isBadRequest());
	}

	@Test
	void signingInGivesANewSessionId() throws Exception {
		MockHttpSession before = new MockHttpSession();
		String idBefore = before.getId();

		MvcResult result = mvc.perform(post("/api/auth/login")
			.session(before)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"username\":\"tester\",\"password\":\"tester-password\"}"))
			.andExpect(status().isOk())
			.andReturn();

		assertThat(result.getRequest().getSession(false).getId()).isNotEqualTo(idBefore);
	}

	@Test
	void signingOutWithoutASessionIsStill204() throws Exception {
		mvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent());
	}

	@Test
	void documentsCanBeDeletedOnceSignedIn() throws Exception {
		// DELETE is not exempt from anything: it works with the session, as the console needs
		mvc.perform(delete("/api/documents/doc_missing").session(signedInSession())).andExpect(status().isNoContent());
	}

	// --- sign-up ---------------------------------------------------------------------------

	@Test
	void signUpStoresAHashedAccountAndSignsStraightIn() throws Exception {
		String name = freshName();

		MvcResult result = signup(name, "a-long-password")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.username").value(name))
			.andReturn();

		UserAccount stored = users.findByUsername(name).orElseThrow();
		assertThat(stored.passwordHash()).startsWith("$2a$").isNotEqualTo("a-long-password");
		assertThat(stored.role()).isEqualTo("USER");
		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
		mvc.perform(get("/api/auth/me").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value(name));
	}

	@Test
	void aSignedUpAccountCanLogInLaterButNotWithAWrongPassword() throws Exception {
		String name = freshName();
		signup(name, "a-long-password").andExpect(status().isCreated());

		login(name, "a-long-password").andExpect(status().isOk());
		login(name, "not-the-password").andExpect(status().isUnauthorized());
	}

	@Test
	void theSameUsernameTwiceIs409() throws Exception {
		String name = freshName();
		signup(name, "a-long-password").andExpect(status().isCreated());

		signup(name, "another-password")
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Username taken"));
	}

	@Test
	void signUpChecksTheInputFirst() throws Exception {
		signup("ab", "a-long-password").andExpect(status().isBadRequest());
		signup("has space", "a-long-password").andExpect(status().isBadRequest());
		signup(freshName(), "short")
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("password: at least 8 characters"));
		assertThat(users.findByUsername("ab")).isEmpty();
	}
}
