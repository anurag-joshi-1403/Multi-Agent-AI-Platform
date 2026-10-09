package com.project.multi_agent_ai_platform.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * A copied {@code .env.example} sets every key to an empty string. The app must still start - with
 * no provider active and the default order - instead of failing on boot.
 */
@SpringBootTest(properties = { "platform.ai.gemini.api-key=", "platform.ai.openai.api-key=",
		"platform.ai.anthropic.api-key=", "platform.ai.groq.api-key=", "platform.ai.openrouter.api-key=",
		"platform.ai.providers=" })
class EmptyKeysStartupTest {

	@Autowired
	ProviderChain chain;

	@Test
	void startsWithNoProviderActiveAndTheDefaultOrder() {
		assertThat(chain.anyActive()).isFalse();
		assertThat(chain.providers()).hasSize(5);
	}
}
