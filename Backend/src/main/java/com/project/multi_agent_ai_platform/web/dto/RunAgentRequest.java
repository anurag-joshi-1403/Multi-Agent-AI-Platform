package com.project.multi_agent_ai_platform.web.dto;

import java.util.Map;

import com.project.multi_agent_ai_platform.agent.core.AgentRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/agents/{id}/run}.
 *
 * @param conversationId groups turns; omit for a single-shot call and the server will mint one
 * @param message        the user message
 * @param attributes     agent-specific extras, e.g. {@code {"documentId": "doc_..."}}
 */
public record RunAgentRequest(
		@Size(max = 128) String conversationId,
		@NotBlank(message = "message must not be blank") @Size(max = 32_000, message = "message is too long (max 32000 characters)") String message,
		Map<String, Object> attributes) {

	public AgentRequest toAgentRequest(String effectiveConversationId) {
		return new AgentRequest(effectiveConversationId, message, attributes);
	}
}
