package com.project.multi_agent_ai_platform.agent.core;

/** Thrown when a request names an agent id that is not registered. Mapped to HTTP 404 at the edge. */
public class UnknownAgentException extends RuntimeException {

	private final String agentId;

	public UnknownAgentException(String agentId) {
		super("No agent registered with id '" + agentId + "'");
		this.agentId = agentId;
	}

	public String getAgentId() {
		return agentId;
	}
}
