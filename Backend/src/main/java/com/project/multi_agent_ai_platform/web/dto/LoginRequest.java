package com.project.multi_agent_ai_platform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of {@code POST /api/auth/login}. */
public record LoginRequest(
		@NotBlank(message = "username must not be blank") @Size(max = 100) String username,
		@NotBlank(message = "password must not be blank") @Size(max = 200) String password) {

	/** Never print the password, e.g. in a log line or an exception message. */
	@Override
	public String toString() {
		return "LoginRequest[username=" + username + ", password=***]";
	}
}
