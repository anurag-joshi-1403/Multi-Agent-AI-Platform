package com.project.multi_agent_ai_platform.config;

/**
 * The chat provider the agents call, resolved once at startup. The key itself is never kept here -
 * only whether one is configured - so this is safe to show in the console.
 * <p>
 * Gemini is the only provider until Phase 6 adds a failover chain.
 *
 * @param id               provider id, e.g. {@code google-genai}
 * @param displayName      human name, e.g. {@code Google Gemini}
 * @param model            chat model id in use
 * @param keyEnvVar        environment variable that carries the key, so errors can say what to set
 * @param apiKeyConfigured {@code false} while the key is blank or the placeholder
 */
public record LlmProvider(String id, String displayName, String model, String keyEnvVar, boolean apiKeyConfigured) {

	/** Default in application.properties, so the app boots without a key. */
	static final String MISSING_API_KEY = "missing-api-key";

	public static LlmProvider gemini(String model, String apiKey) {
		boolean configured = apiKey != null && !apiKey.isBlank() && !MISSING_API_KEY.equals(apiKey);
		return new LlmProvider("google-genai", "Google Gemini", model == null ? "" : model, "GEMINI_API_KEY",
				configured);
	}
}
