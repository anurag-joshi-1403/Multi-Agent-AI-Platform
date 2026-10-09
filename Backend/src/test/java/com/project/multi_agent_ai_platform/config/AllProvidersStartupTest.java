package com.project.multi_agent_ai_platform.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Every provider has a (fake) key, so AiConfig builds all five clients - Gemini, OpenAI, Anthropic,
 * and OpenAI pointed at Groq and OpenRouter. Building one makes no network call; this proves each
 * builds, and that a custom order is honoured.
 */
@SpringBootTest(properties = { "platform.ai.providers=anthropic, openai, google-genai, openrouter, groq",
		"platform.ai.groq.api-key=fake", "platform.ai.openrouter.api-key=fake", "platform.ai.openai.api-key=fake",
		"platform.ai.anthropic.api-key=fake" })
class AllProvidersStartupTest {

	@Autowired
	ProviderChain chain;

	@Test
	void everyKeyedProviderIsBuiltAndActiveInTheConfiguredOrder() {
		assertThat(chain.active()).extracting(LlmProvider::id)
			.containsExactly("anthropic", "openai", "google-genai", "openrouter", "groq");
		assertThat(chain.primary().displayName()).isEqualTo("Anthropic Claude");
	}
}
