package com.project.multi_agent_ai_platform.config;

/**
 * What the platform knows about its active chat provider, resolved once at startup from the
 * Spring AI properties. The key itself is never exposed - only whether one is configured.
 *
 * @param provider         the provider selected by {@code spring.ai.model.chat}
 * @param model            the chat model id in use, e.g. {@code gemini-2.5-flash}
 * @param apiKeyConfigured {@code false} while the key is blank or the placeholder
 */
public record LlmProviderInfo(LlmProvider provider, String model, boolean apiKeyConfigured) {

	static final String MISSING_API_KEY = "missing-api-key";

	static LlmProviderInfo of(LlmProvider provider, String model, String apiKey) {
		boolean configured = apiKey != null && !apiKey.isBlank() && !MISSING_API_KEY.equals(apiKey);
		return new LlmProviderInfo(provider, model == null ? "" : model, configured);
	}
}
