package com.project.multi_agent_ai_platform.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * AI provider settings, bound from {@code platform.ai.*}: the failover order and one block per
 * provider. A provider with a blank key is listed but never built or called.
 *
 * @param providers the failover order, from {@code AI_PROVIDERS}
 */
@ConfigurationProperties(prefix = "platform.ai")
public record AiProperties(
		@DefaultValue({ "groq", "openrouter", "google-genai", "openai", "anthropic" }) List<String> providers,
		@DefaultValue Provider groq, @DefaultValue Provider openrouter, @DefaultValue Provider gemini,
		@DefaultValue Provider openai, @DefaultValue Provider anthropic) {

	/**
	 * @param apiKey  the provider's key; blank means "skip this provider"
	 * @param model   model id at that provider
	 * @param baseUrl API root, for OpenAI-compatible providers (Groq, OpenRouter); blank keeps the SDK's
	 */
	public record Provider(@DefaultValue("") String apiKey, @DefaultValue("") String model,
			@DefaultValue("") String baseUrl) {
	}
}
