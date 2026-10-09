package com.project.multi_agent_ai_platform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring shared by every LLM-backed agent. The {@code ChatClient.Builder} itself comes from Spring
 * AI's auto-configuration; this adds a description of the provider behind it and the conversation
 * memory.
 * <p>
 * The {@link ChatMemoryRepository} is Spring AI's in-memory one, so conversations are forgotten
 * on restart. Phase 4 swaps in a JDBC repository; nothing in the agents changes.
 */
@Configuration
@EnableConfigurationProperties(PlatformProperties.class)
public class AiConfig {

	private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

	@Bean
	LlmProvider llmProvider(@Value("${spring.ai.google.genai.chat.options.model:}") String model,
			@Value("${spring.ai.google.genai.api-key:}") String apiKey) {
		return LlmProvider.gemini(model, apiKey);
	}

	/** A sliding window: only the last {@code platform.memory.max-messages} of each conversation are replayed. */
	@Bean
	ChatMemory chatMemory(ChatMemoryRepository repository, PlatformProperties properties) {
		return MessageWindowChatMemory.builder()
			.chatMemoryRepository(repository)
			.maxMessages(properties.memory().maxMessages())
			.build();
	}

	/** Make a missing key loud at startup instead of a cryptic error on the first request. */
	@Bean
	ApplicationListener<ApplicationReadyEvent> providerReport(LlmProvider provider) {
		return event -> {
			if (provider.apiKeyConfigured()) {
				log.info("Chat provider: {} ({})", provider.displayName(), provider.model());
			}
			else {
				log.warn("Chat provider: {} ({}). {} is not set - the API is up, but every agent call will fail "
						+ "with 502 until you add it to Backend/.env and restart.", provider.displayName(),
						provider.model(), provider.keyEnvVar());
			}
		};
	}
}
