package com.project.multi_agent_ai_platform.agent.core;

import java.util.Map;
import java.util.Objects;

/**
 * Input to an {@link Agent}.
 *
 * @param conversationId groups turns of one conversation; may be {@code null} for single-shot calls
 * @param message        the user's message, never blank
 * @param attributes     agent-specific extras (e.g. a document id for the PDF agent); never {@code null}
 */
public record AgentRequest(String conversationId, String message, Map<String, Object> attributes) {

	public AgentRequest {
		Objects.requireNonNull(message, "message must not be null");
		if (message.isBlank()) {
			throw new IllegalArgumentException("message must not be blank");
		}
		attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
	}

	public static AgentRequest of(String message) {
		return new AgentRequest(null, message, Map.of());
	}
}
