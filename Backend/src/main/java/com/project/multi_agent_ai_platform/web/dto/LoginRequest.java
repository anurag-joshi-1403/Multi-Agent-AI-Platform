package com.project.multi_agent_ai_platform.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Body of {@code POST /api/auth/login}. */
public record LoginRequest(@NotBlank(message = "username must not be blank") String username,
		@NotBlank(message = "password must not be blank") String password) {
}
