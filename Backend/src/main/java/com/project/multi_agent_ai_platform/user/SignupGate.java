package com.project.multi_agent_ai_platform.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Who may create an account: anyone, or, once {@code SIGNUP_INVITE_CODE} is set, only people who
 * were given that code. Without it, a console others can reach lets strangers sign up and spend
 * the AI credits behind it.
 */
@Component
public class SignupGate {

	private final byte[] inviteCode;

	public SignupGate(@Value("${platform.signup.invite-code:}") String inviteCode) {
		this.inviteCode = inviteCode.strip().getBytes(StandardCharsets.UTF_8);
	}

	/** True once an invite code is configured; until then sign-up is open. */
	public boolean inviteRequired() {
		return inviteCode.length > 0;
	}

	/**
	 * Lets the sign-up through, or throws when a code is required and {@code given} is missing or
	 * wrong. The comparison takes the same time however much of the code matches, so the code can't
	 * be guessed one character at a time.
	 */
	public void check(String given) {
		if (!inviteRequired()) {
			return;
		}
		byte[] attempt = given == null ? new byte[0] : given.strip().getBytes(StandardCharsets.UTF_8);
		if (!MessageDigest.isEqual(inviteCode, attempt)) {
			throw new InvalidInviteCodeException();
		}
	}
}
