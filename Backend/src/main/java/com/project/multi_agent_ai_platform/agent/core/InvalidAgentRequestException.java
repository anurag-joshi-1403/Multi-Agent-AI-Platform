package com.project.multi_agent_ai_platform.agent.core;

/**
 * Thrown by an agent when the request is well-formed HTTP-wise but unusable for that agent,
 * e.g. the document agent was called without a {@code documentId} attribute. Mapped to HTTP 400.
 */
public class InvalidAgentRequestException extends RuntimeException {

	public InvalidAgentRequestException(String message) {
		super(message);
	}
}
