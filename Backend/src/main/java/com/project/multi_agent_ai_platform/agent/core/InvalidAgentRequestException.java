package com.project.multi_agent_ai_platform.agent.core;

/**
 * Thrown by an agent when a request is valid JSON but unusable for that agent, e.g. the Document
 * Agent called without an attached file. Mapped to HTTP 400.
 */
public class InvalidAgentRequestException extends RuntimeException {

	public InvalidAgentRequestException(String message) {
		super(message);
	}
}
