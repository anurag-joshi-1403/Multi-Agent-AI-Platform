package com.project.multi_agent_ai_platform.web.dto;

/**
 * Body of {@code GET /api/platform}: enough for a UI to explain the backend's state without
 * leaking secrets.
 *
 * @param provider         provider id as accepted by {@code AI_PROVIDER}, e.g. {@code google-genai}
 * @param providerName     human name, e.g. {@code Google Gemini}
 * @param model            chat model id in use
 * @param apiKeyConfigured whether the provider key is set (never the key itself)
 * @param keyEnvVar        which environment variable carries the key
 * @param agents           number of registered agents
 * @param memoryMaxMessages messages replayed per conversation
 * @param documents        document store limits and current count
 */
public record PlatformStatus(String provider, String providerName, String model, boolean apiKeyConfigured,
		String keyEnvVar, int agents, int memoryMaxMessages, Documents documents) {

	public record Documents(int stored, int maxStored, int maxContextChars) {
	}
}
