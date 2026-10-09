package com.project.multi_agent_ai_platform.agent.core;

import java.util.List;

/**
 * Contract every agent implements.
 * <p>
 * Implementations are Spring beans ({@code @Component}); {@link AgentRegistry} discovers them
 * automatically, so adding an agent never requires editing the registry, the orchestrator or the
 * console - the console builds its cards and controls from what the agent says about itself here.
 */
public interface Agent {

	/** Stable, URL-safe identifier, e.g. {@code "coding"}. Must be unique. */
	String id();

	/** One-line, human-readable capability summary. */
	String description();

	/** Display name for the console. Defaults to a title-cased id, e.g. {@code "Coding Agent"}. */
	default String name() {
		String id = id();
		return Character.toUpperCase(id.charAt(0)) + id.substring(1) + " Agent";
	}

	/** Short capability tags, e.g. {@code ["Generate", "Explain"]}. Optional. */
	default List<String> capabilities() {
		return List.of();
	}

	/** The request attributes this agent understands, so the console can offer proper controls. Optional. */
	default List<AgentParameter> parameters() {
		return List.of();
	}

	/** Handle one request synchronously. Implementations should not swallow provider errors. */
	AgentResponse handle(AgentRequest request);
}
