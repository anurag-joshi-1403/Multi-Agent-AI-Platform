package com.project.multi_agent_ai_platform.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LlmProviderTest {

	@Test
	void geminiWithARealKeyIsConfigured() {
		LlmProvider provider = LlmProvider.gemini("gemini-x", "some-key");

		assertThat(provider.id()).isEqualTo("google-genai");
		assertThat(provider.displayName()).isEqualTo("Google Gemini");
		assertThat(provider.model()).isEqualTo("gemini-x");
		assertThat(provider.keyEnvVar()).isEqualTo("GEMINI_API_KEY");
		assertThat(provider.apiKeyConfigured()).isTrue();
	}

	@Test
	void placeholderBlankOrMissingKeyIsNotConfigured() {
		assertThat(LlmProvider.gemini("m", LlmProvider.MISSING_API_KEY).apiKeyConfigured()).isFalse();
		assertThat(LlmProvider.gemini("m", "  ").apiKeyConfigured()).isFalse();
		assertThat(LlmProvider.gemini("m", null).apiKeyConfigured()).isFalse();
	}

	@Test
	void missingModelBecomesEmpty() {
		assertThat(LlmProvider.gemini(null, "k").model()).isEmpty();
	}
}
