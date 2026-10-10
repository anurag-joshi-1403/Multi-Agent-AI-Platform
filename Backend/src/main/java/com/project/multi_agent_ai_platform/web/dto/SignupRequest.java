package com.project.multi_agent_ai_platform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body of POST /api/auth/signup. */
public record SignupRequest(
		@NotBlank @Size(min = 3, max = 50)
		@Pattern(regexp = "[A-Za-z0-9._-]+", message = "letters, digits, dot, dash and underscore only") String username,
		@NotBlank @Size(min = 8, max = 200, message = "at least 8 characters") String password) {

	/** Never print the password, e.g. in a log line. */
	@Override
	public String toString() {
		return "SignupRequest[username=" + username + ", password=***]";
	}
}
