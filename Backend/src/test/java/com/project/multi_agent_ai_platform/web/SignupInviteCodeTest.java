package com.project.multi_agent_ai_platform.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.project.multi_agent_ai_platform.user.UserStore;

/** Sign-up with an invite code configured, against the whole application (MongoDB must be running). */
@SpringBootTest(properties = "platform.signup.invite-code=let-me-in")
@AutoConfigureMockMvc
class SignupInviteCodeTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	UserStore users;

	private ResultActions signup(String username, String inviteCode) throws Exception {
		String code = inviteCode == null ? "" : ",\"inviteCode\":\"" + inviteCode + "\"";
		return mvc.perform(post("/api/auth/signup")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"username\":\"" + username + "\",\"password\":\"a-long-password\"" + code + "}"));
	}

	private static String freshName() {
		return "inv-" + UUID.randomUUID().toString().substring(0, 8);
	}

	@Test
	void withoutTheCodeNoAccountIsCreated() throws Exception {
		String name = freshName();

		for (String code : new String[] { null, "", "wrong-code" }) {
			signup(name, code).andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Invite code required"))
				.andExpect(jsonPath("$.detail").value("Sign-up needs a valid invite code. Ask whoever runs this console for one."));
		}
		assertThat(users.findByUsername(name)).isEmpty();
	}

	@Test
	void theRightCodeCreatesTheAccountAndSignsIn() throws Exception {
		String name = freshName();

		signup(name, "let-me-in").andExpect(status().isCreated()).andExpect(jsonPath("$.username").value(name));

		assertThat(users.findByUsername(name)).isPresent();
	}

	@Test
	void withoutTheCodeTakenUsernamesStayHidden() throws Exception {
		String name = freshName();
		signup(name, "let-me-in").andExpect(status().isCreated());

		// The same name again: 403, not 409, so a stranger can't tell which usernames exist
		signup(name, "guess").andExpect(status().isForbidden());
		signup(name, "let-me-in").andExpect(status().isConflict());
	}
}
