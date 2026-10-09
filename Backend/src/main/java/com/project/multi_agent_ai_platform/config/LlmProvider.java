package com.project.multi_agent_ai_platform.config;

/**
 * One AI provider in the failover chain, as the console sees it. The key itself is never kept
 * here - only whether one is configured - so this is safe to show.
 *
 * @param id               provider id as used in {@code AI_PROVIDERS}, e.g. {@code google-genai}
 * @param displayName      human name, e.g. {@code Google Gemini}
 * @param model            chat model id in use
 * @param keyEnvVar        environment variable that carries the key, so errors can say what to set
 * @param apiKeyConfigured {@code false} while the key is blank; the chain skips the provider then
 */
public record LlmProvider(String id, String displayName, String model, String keyEnvVar, boolean apiKeyConfigured) {

	public static LlmProvider of(String id, String displayName, String keyEnvVar, String model, String apiKey) {
		return new LlmProvider(id, displayName, model == null ? "" : model, keyEnvVar,
				apiKey != null && !apiKey.isBlank());
	}

	public static LlmProvider gemini(String model, String apiKey) {
		return of("google-genai", "Google Gemini", "GEMINI_API_KEY", model, apiKey);
	}
}
