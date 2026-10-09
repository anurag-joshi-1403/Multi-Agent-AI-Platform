package com.project.multi_agent_ai_platform.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.retry.RetryTemplate;

import com.google.genai.Client;

import io.micrometer.observation.ObservationRegistry;

/**
 * Wiring shared by every LLM-backed agent: the provider chain they all call, and the conversation
 * memory.
 * <p>
 * Spring AI's own per-provider setup is switched off ({@code spring.ai.model.chat=none}): it fails
 * to start when a key is empty. Instead a client is built here for each provider that has a key,
 * and only for those. The chain is the one {@code ChatModel} bean, so the {@code ChatClient.Builder}
 * the agents receive talks to it.
 */
@Configuration
@EnableConfigurationProperties({ PlatformProperties.class, AiProperties.class })
public class AiConfig {

	private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

	static final List<String> KNOWN_PROVIDERS = List.of("groq", "openrouter", "google-genai", "openai", "anthropic");

	private static final double TEMPERATURE = 0.2;

	private static final int MAX_TOKENS = 8192;

	/** One retry inside a provider, then on to the next one rather than waiting. */
	private static final int MAX_RETRIES = 1;

	private static final Duration TIMEOUT = Duration.ofSeconds(60);

	@Bean
	@Primary
	ProviderChain providerChain(AiProperties ai, ObjectProvider<RetryTemplate> retryTemplate,
			ObjectProvider<ObservationRegistry> observations) {
		ObservationRegistry observationRegistry = observations.getIfUnique(() -> ObservationRegistry.NOOP);
		List<ProviderChain.Link> links = new ArrayList<>();
		for (String id : order(ai.providers())) {
			links.add(switch (id) {
				case "groq" -> link("groq", "Groq", "GROQ_API_KEY", ai.groq(),
						() -> openAiCompatible(ai.groq(), observationRegistry));
				case "openrouter" -> link("openrouter", "OpenRouter", "OPENROUTER_API_KEY", ai.openrouter(),
						() -> openAiCompatible(ai.openrouter(), observationRegistry));
				case "google-genai" -> link("google-genai", "Google Gemini", "GEMINI_API_KEY", ai.gemini(),
						() -> gemini(ai.gemini(), retryTemplate.getIfAvailable(() -> RetryUtils.SHORT_RETRY_TEMPLATE),
								observationRegistry));
				case "openai" -> link("openai", "OpenAI", "OPENAI_API_KEY", ai.openai(),
						() -> openAiCompatible(ai.openai(), observationRegistry));
				case "anthropic" -> link("anthropic", "Anthropic Claude", "ANTHROPIC_API_KEY", ai.anthropic(),
						() -> anthropic(ai.anthropic(), observationRegistry));
				default -> throw new IllegalStateException("unreachable: " + id);
			});
		}
		return new ProviderChain(links);
	}

	/** A sliding window: only the last {@code platform.memory.max-messages} of each conversation are replayed. */
	@Bean
	ChatMemory chatMemory(ChatMemoryRepository repository, PlatformProperties properties) {
		return MessageWindowChatMemory.builder()
			.chatMemoryRepository(repository)
			.maxMessages(properties.memory().maxMessages())
			.build();
	}

	/** One line at startup with the whole chain, so a missing key shows before the first request does. */
	@Bean
	ApplicationListener<ApplicationReadyEvent> providerReport(ProviderChain chain) {
		return event -> {
			String order = chain.providers()
				.stream()
				.map(p -> p.apiKeyConfigured() ? p.displayName() + " (" + p.model() + ")"
						: p.displayName() + " [skipped: no " + p.keyEnvVar() + "]")
				.collect(Collectors.joining(" -> "));
			if (chain.anyActive()) {
				log.info("AI providers, in failover order: {}", order);
			}
			else {
				log.warn("No AI provider has a key, so every agent call will fail until one is set in Backend/.env "
						+ "(e.g. GEMINI_API_KEY) and the backend is restarted. Chain: {}", order);
			}
		};
	}

	/**
	 * {@code AI_PROVIDERS} as a clean list: trimmed, lower-case; empty (e.g. {@code AI_PROVIDERS=} in
	 * .env) means the default order. An unknown or repeated name stops startup rather than quietly
	 * dropping a provider someone expected to be tried.
	 */
	static List<String> order(List<String> configured) {
		List<String> ids = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (String raw : configured) {
			String id = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
			if (id.isEmpty()) {
				continue;
			}
			if (!KNOWN_PROVIDERS.contains(id)) {
				throw new IllegalStateException(
						"AI_PROVIDERS: unknown provider '" + id + "'. Use any of " + String.join(", ", KNOWN_PROVIDERS));
			}
			if (!seen.add(id)) {
				throw new IllegalStateException("AI_PROVIDERS lists '" + id + "' more than once");
			}
			ids.add(id);
		}
		return ids.isEmpty() ? KNOWN_PROVIDERS : ids;
	}

	/** The client is only built for a provider that has a key; the rest are listed but never called. */
	private static ProviderChain.Link link(String id, String name, String keyEnvVar, AiProperties.Provider settings,
			Supplier<ChatModel> client) {
		LlmProvider provider = LlmProvider.of(id, name, keyEnvVar, settings.model(), settings.apiKey());
		return new ProviderChain.Link(provider, provider.apiKeyConfigured() ? client.get() : null);
	}

	/** OpenAI itself, and Groq and OpenRouter, which speak the same API at their own address. */
	private static ChatModel openAiCompatible(AiProperties.Provider p, ObservationRegistry observations) {
		OpenAiChatOptions.Builder options = OpenAiChatOptions.builder()
			.apiKey(p.apiKey())
			.model(p.model())
			.temperature(TEMPERATURE)
			.maxRetries(MAX_RETRIES)
			.timeout(TIMEOUT);
		if (!p.baseUrl().isBlank()) {
			options.baseUrl(p.baseUrl());
		}
		return OpenAiChatModel.builder().options(options.build()).observationRegistry(observations).build();
	}

	/** Gemini retries through Spring AI's retry template ({@code spring.ai.retry.*}), not its SDK. */
	private static ChatModel gemini(AiProperties.Provider p, RetryTemplate retryTemplate,
			ObservationRegistry observations) {
		return GoogleGenAiChatModel.builder()
			.genAiClient(Client.builder().apiKey(p.apiKey()).build())
			.options(GoogleGenAiChatOptions.builder()
				.model(p.model())
				.temperature(TEMPERATURE)
				.maxOutputTokens(MAX_TOKENS)
				.build())
			.retryTemplate(retryTemplate)
			.observationRegistry(observations)
			.build();
	}

	/** No temperature: newer Claude models reject it. */
	private static ChatModel anthropic(AiProperties.Provider p, ObservationRegistry observations) {
		AnthropicChatOptions.Builder options = AnthropicChatOptions.builder()
			.apiKey(p.apiKey())
			.model(p.model())
			.maxTokens(MAX_TOKENS)
			.maxRetries(MAX_RETRIES)
			.timeout(TIMEOUT);
		if (!p.baseUrl().isBlank()) {
			options.baseUrl(p.baseUrl());
		}
		return AnthropicChatModel.builder().options(options.build()).observationRegistry(observations).build();
	}
}
