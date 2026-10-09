package com.project.multi_agent_ai_platform.web.dto;

/** Body of {@code POST /api/auth/login} and {@code GET /api/auth/me}: who is signed in. */
public record AuthResponse(String username) {
}
