package com.project.multi_agent_ai_platform.web.dto;

/** Response of {@code POST /api/auth/login} and {@code GET /api/auth/me}. */
public record AuthResponse(String username) {
}
