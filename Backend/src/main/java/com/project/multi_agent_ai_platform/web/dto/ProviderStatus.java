package com.project.multi_agent_ai_platform.web.dto;

import com.project.multi_agent_ai_platform.config.LlmProvider;

/**
 * One entry of the failover chain in {@code GET /api/platform} ({@code ProviderStatus} in the frontend).
 *
 * @param active {@code false} when its key is not set, so the chain skips it
 */
public record ProviderStatus(String provider, String providerName, String model, boolean active, String keyEnvVar) {

	public static ProviderStatus of(LlmProvider p) {
		return new ProviderStatus(p.id(), p.displayName(), p.model(), p.apiKeyConfigured(), p.keyEnvVar());
	}
}
