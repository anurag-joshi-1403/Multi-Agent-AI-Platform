package com.project.multi_agent_ai_platform.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SignupGateTest {

	@Test
	void withoutACodeSignUpIsOpenToAnyone() {
		SignupGate open = new SignupGate("  ");

		assertThat(open.inviteRequired()).isFalse();
		assertThatCode(() -> open.check(null)).doesNotThrowAnyException();
		assertThatCode(() -> open.check("anything")).doesNotThrowAnyException();
	}

	@Test
	void withACodeOnlyThatCodeGetsIn() {
		SignupGate gate = new SignupGate("let-me-in");

		assertThat(gate.inviteRequired()).isTrue();
		assertThatCode(() -> gate.check("let-me-in")).doesNotThrowAnyException();
		// Spaces around a pasted code are forgiven, case is not
		assertThatCode(() -> gate.check("  let-me-in ")).doesNotThrowAnyException();
		for (String wrong : new String[] { null, "", "let-me", "let-me-in-please", "LET-ME-IN" }) {
			assertThatThrownBy(() -> gate.check(wrong)).isInstanceOf(InvalidInviteCodeException.class);
		}
	}
}
