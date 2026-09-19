package com.project.multi_agent_ai_platform.config;

import java.util.Locale;

/**
 * The chat provider selected by {@code spring.ai.model.chat} (env {@code AI_PROVIDER}), with the
 * environment variable that carries its key so error messages can say exactly what to set.
 */
public enum LlmProvider {

	GEMINI("google-genai", "Google Gemini", "GEMINI_API_KEY"),
	ANTHROPIC("anthropic", "Anthropic Claude", "ANTHROPIC_API_KEY"),
	OPENAI("openai", "OpenAI", "OPENAI_API_KEY"),
	UNKNOWN("unknown", "the LLM provider", "the provider API key");

	private final String id;

	private final String displayName;

	private final String keyEnvVar;

	LlmProvider(String id, String displayName, String keyEnvVar) {
		this.id = id;
		this.displayName = displayName;
		this.keyEnvVar = keyEnvVar;
	}

	/** Value of {@code spring.ai.model.chat}. */
	public String id() {
		return id;
	}

	public String displayName() {
		return displayName;
	}

	/** Environment variable the key is read from, e.g. {@code GEMINI_API_KEY}. */
	public String keyEnvVar() {
		return keyEnvVar;
	}

	public static LlmProvider fromId(String id) {
		if (id == null) {
			return UNKNOWN;
		}
		String normalised = id.trim().toLowerCase(Locale.ROOT);
		for (LlmProvider provider : values()) {
			if (provider.id.equals(normalised)) {
				return provider;
			}
		}
		return UNKNOWN;
	}
}
