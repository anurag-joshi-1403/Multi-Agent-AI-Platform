package com.project.multi_agent_ai_platform.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/** How {@code AI_PROVIDERS} is read. */
class AiConfigTest {

	@Test
	void namesAreTrimmedAndLowerCasedInTheGivenOrder() {
		assertThat(AiConfig.order(List.of(" Google-GenAI ", "groq", ""))).containsExactly("google-genai", "groq");
	}

	@Test
	void emptyMeansTheDefaultOrder() {
		assertThat(AiConfig.order(List.of())).containsExactly("groq", "openrouter", "google-genai", "openai", "anthropic");
		assertThat(AiConfig.order(Arrays.asList(" ", null))).isEqualTo(AiConfig.KNOWN_PROVIDERS);
	}

	@Test
	void anUnknownOrRepeatedNameStopsStartup() {
		assertThatThrownBy(() -> AiConfig.order(List.of("gemini")))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("unknown provider 'gemini'")
			.hasMessageContaining("google-genai");
		assertThatThrownBy(() -> AiConfig.order(List.of("groq", "GROQ")))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("more than once");
	}
}
