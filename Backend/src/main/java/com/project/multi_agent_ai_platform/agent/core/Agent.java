package com.project.multi_agent_ai_platform.agent.core;

/**
 * Contract every specialised agent implements.
 * <p>
 * Implementations are Spring beans ({@code @Component}); {@link AgentRegistry}
 * discovers them automatically, so adding an agent never requires editing the
 * registry or the orchestrator.
 */
public interface Agent {

	/** Stable, URL-safe identifier, e.g. {@code "coding"}. Must be unique. */
	String id();

	/** One-line, human-readable capability summary. Later used by an LLM router to pick an agent. */
	String description();

	/** Handle a single request synchronously. Implementations should not swallow provider errors. */
	AgentResponse handle(AgentRequest request);
}
