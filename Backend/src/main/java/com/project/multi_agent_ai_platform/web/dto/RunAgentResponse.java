package com.project.multi_agent_ai_platform.web.dto;

import java.util.Map;

import com.project.multi_agent_ai_platform.agent.core.AgentResponse;

/**
 * Body returned by {@code POST /api/agents/{id}/run}: the {@link AgentResponse} fields plus the
 * conversation id that was used (important when the server minted it) and server-side latency.
 */
public record RunAgentResponse(String agentId, String content, Map<String, Object> metadata, String conversationId,
		long elapsedMs) {

	public static RunAgentResponse of(AgentResponse response, String conversationId, long elapsedMs) {
		return new RunAgentResponse(response.agentId(), response.content(), response.metadata(), conversationId,
				elapsedMs);
	}
}
