package com.project.multi_agent_ai_platform.web.dto;

import java.util.List;

/**
 * Body of {@code GET /api/platform}: enough for the console to explain the backend's state without
 * leaking secrets.
 *
 * @param provider          id of the provider tried first (the first with a key)
 * @param providerName      its human name, e.g. {@code Google Gemini}
 * @param model             its chat model id
 * @param apiKeyConfigured  whether any provider in the chain has a key (never a key itself)
 * @param keyEnvVar         the first provider's key variable
 * @param agents            number of registered agents
 * @param memoryMaxMessages messages replayed per conversation
 * @param documents         uploads stored now, and the store's limits
 * @param providers         the whole failover chain in order, including providers skipped for lack of a key
 */
public record PlatformStatus(String provider, String providerName, String model, boolean apiKeyConfigured,
		String keyEnvVar, int agents, int memoryMaxMessages, Documents documents, List<ProviderStatus> providers) {

	public record Documents(int stored, int maxStored, int maxContextChars) {
	}
}
