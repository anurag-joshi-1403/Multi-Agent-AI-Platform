package com.project.multi_agent_ai_platform.agent.core;

import java.util.List;
import java.util.Objects;

/**
 * Describes one entry an agent understands in {@link AgentRequest#attributes()}, so a UI can
 * render a proper control instead of asking for raw JSON.
 *
 * @param name         attribute key, e.g. {@code "language"}
 * @param label        short human label
 * @param description  one-line help text
 * @param type         how to render / validate it
 * @param required     whether the agent refuses requests without it
 * @param options      allowed values for {@link Type#SELECT}; empty otherwise
 * @param defaultValue value used when the attribute is absent; may be {@code null}
 */
public record AgentParameter(String name, String label, String description, Type type, boolean required,
		List<String> options, Object defaultValue) {

	public enum Type {
		/** Free text. */
		STRING,
		/** Integer. */
		NUMBER,
		/** One of {@link #options}. */
		SELECT,
		/** Id of a document uploaded via {@code /api/documents}. */
		DOCUMENT
	}

	public AgentParameter {
		Objects.requireNonNull(name, "name must not be null");
		Objects.requireNonNull(label, "label must not be null");
		Objects.requireNonNull(type, "type must not be null");
		description = description == null ? "" : description;
		options = options == null ? List.of() : List.copyOf(options);
	}

	public static AgentParameter string(String name, String label, String description) {
		return new AgentParameter(name, label, description, Type.STRING, false, List.of(), null);
	}

	public static AgentParameter number(String name, String label, String description, int defaultValue) {
		return new AgentParameter(name, label, description, Type.NUMBER, false, List.of(), defaultValue);
	}

	public static AgentParameter select(String name, String label, String description, List<String> options,
			String defaultValue) {
		return new AgentParameter(name, label, description, Type.SELECT, false, options, defaultValue);
	}

	public static AgentParameter document(String name, String label, String description) {
		return new AgentParameter(name, label, description, Type.DOCUMENT, true, List.of(), null);
	}
}
