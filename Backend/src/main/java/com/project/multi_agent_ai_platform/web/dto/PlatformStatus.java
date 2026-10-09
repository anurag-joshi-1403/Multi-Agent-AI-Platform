package com.project.multi_agent_ai_platform.web.dto;

/**
 * Body of {@code GET /api/platform}: enough for the console to explain the backend's state without
 * leaking secrets.
 *
 * @param provider          provider id, e.g. {@code google-genai}
 * @param providerName      human name, e.g. {@code Google Gemini}
 * @param model             chat model id in use
 * @param apiKeyConfigured  whether the provider key is set (never the key itself)
 * @param keyEnvVar         which environment variable carries the key
 * @param agents            number of registered agents
 * @param memoryMaxMessages messages replayed per conversation ({@code 0} until memory arrives in Phase 3)
 * @param documents         document store limits and count (all {@code 0} until Phase 4)
 */
public record PlatformStatus(String provider, String providerName, String model, boolean apiKeyConfigured,
		String keyEnvVar, int agents, int memoryMaxMessages, Documents documents) {

	public record Documents(int stored, int maxStored, int maxContextChars) {

		public static final Documents NONE = new Documents(0, 0, 0);
	}
}
