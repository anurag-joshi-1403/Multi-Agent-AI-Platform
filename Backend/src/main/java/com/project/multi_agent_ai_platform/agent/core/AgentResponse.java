package com.project.multi_agent_ai_platform.agent.core;

import java.util.Map;
import java.util.Objects;

/**
 * Output of an {@link Agent}.
 *
 * @param agentId  which agent produced this
 * @param content  the answer, as markdown
 * @param metadata anything secondary: provider, model, token usage; never {@code null}
 */
public record AgentResponse(String agentId, String content, Map<String, Object> metadata) {

	public AgentResponse {
		Objects.requireNonNull(agentId, "agentId must not be null");
		Objects.requireNonNull(content, "content must not be null");
		metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
	}

	public static AgentResponse of(String agentId, String content) {
		return new AgentResponse(agentId, content, Map.of());
	}
}
