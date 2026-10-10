package com.project.multi_agent_ai_platform.user;

/** Sign-up with a username that already exists. Mapped to HTTP 409. */
public class UsernameTakenException extends RuntimeException {

	public UsernameTakenException(String username) {
		super("Username '" + username + "' is already registered");
	}
}
