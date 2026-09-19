package com.project.multi_agent_ai_platform.agent.core;

import java.util.List;

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

	/** Display name for UIs. Defaults to a title-cased id, e.g. {@code "Coding Agent"}. */
	default String name() {
		String id = id();
		return Character.toUpperCase(id.charAt(0)) + id.substring(1) + " Agent";
	}

	/** Short capability tags for UIs, e.g. {@code ["Generate", "Explain"]}. Optional. */
	default List<String> capabilities() {
		return List.of();
	}

	/** The request attributes this agent understands, so a UI can offer proper controls. Optional. */
	default List<AgentParameter> parameters() {
		return List.of();
	}

	/** Handle a single request synchronously. Implementations should not swallow provider errors. */
	AgentResponse handle(AgentRequest request);
}
