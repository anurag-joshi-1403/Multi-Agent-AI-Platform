package com.project.multi_agent_ai_platform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Wiring shared by every LLM-backed agent: a sliding-window conversation memory and a
 * description of the active provider. The {@link ChatMemoryRepository} is Spring AI's in-memory
 * one until a database arrives in a later phase; swapping it changes nothing in the agents.
 */
@Configuration
@EnableConfigurationProperties(PlatformProperties.class)
public class AiConfig {

	private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

	@Bean
	ChatMemory chatMemory(ChatMemoryRepository repository, PlatformProperties properties) {
		return MessageWindowChatMemory.builder()
			.chatMemoryRepository(repository)
			.maxMessages(properties.memory().maxMessages())
			.build();
	}

	/** Which chat provider {@code spring.ai.model.chat} selected, for log and error messages. */
	@Bean
	LlmProvider llmProvider(@Value("${spring.ai.model.chat:unknown}") String providerId) {
		return LlmProvider.fromId(providerId);
	}

	@Bean
	LlmProviderInfo llmProviderInfo(LlmProvider provider,
			@Value("${spring.ai.google.genai.api-key:}") String geminiKey,
			@Value("${spring.ai.google.genai.chat.options.model:}") String geminiModel,
			@Value("${spring.ai.anthropic.api-key:}") String anthropicKey,
			@Value("${spring.ai.anthropic.chat.options.model:}") String anthropicModel,
			@Value("${spring.ai.openai.api-key:}") String openAiKey,
			@Value("${spring.ai.openai.chat.options.model:}") String openAiModel) {
		return switch (provider) {
			case GEMINI -> LlmProviderInfo.of(provider, geminiModel, geminiKey);
			case ANTHROPIC -> LlmProviderInfo.of(provider, anthropicModel, anthropicKey);
			case OPENAI -> LlmProviderInfo.of(provider, openAiModel, openAiKey);
			case UNKNOWN -> LlmProviderInfo.of(provider, "", "");
		};
	}

	/** Make a missing key loud at startup instead of a cryptic error on the first request. */
	@Bean
	StartupReport startupReport(LlmProviderInfo info) {
		return new StartupReport(info);
	}

	record StartupReport(LlmProviderInfo info) {

		@EventListener(ApplicationReadyEvent.class)
		void onReady() {
			LlmProvider provider = info.provider();
			if (provider == LlmProvider.UNKNOWN) {
				log.warn("spring.ai.model.chat / AI_PROVIDER is not one of google-genai, anthropic, openai.");
				return;
			}
			if (info.apiKeyConfigured()) {
				log.info("Chat provider: {} ({})", provider.displayName(), info.model());
			}
			else {
				log.warn("Chat provider: {} ({}). {} is not set - the API is up, but every agent call will fail "
						+ "with 502 until you export {} and restart.", provider.displayName(), info.model(),
						provider.keyEnvVar(), provider.keyEnvVar());
			}
		}
	}
}
