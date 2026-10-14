package com.project.multi_agent_ai_platform.user;

/** Sign-up without the right invite code, while one is required. Mapped to HTTP 403. */
public class InvalidInviteCodeException extends RuntimeException {

	public InvalidInviteCodeException() {
		super("Sign-up needs a valid invite code");
	}
}
