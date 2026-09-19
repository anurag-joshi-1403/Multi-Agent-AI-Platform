package com.project.multi_agent_ai_platform.web.dto;

import java.util.List;

import com.project.multi_agent_ai_platform.agent.core.Agent;
import com.project.multi_agent_ai_platform.agent.core.AgentParameter;

/** What {@code GET /api/agents} returns for each registered agent. */
public record AgentSummary(String id, String name, String description, List<String> capabilities,
		List<AgentParameter> parameters) {

	public static AgentSummary of(Agent agent) {
		return new AgentSummary(agent.id(), agent.name(), agent.description(), agent.capabilities(), agent.parameters());
	}
}
