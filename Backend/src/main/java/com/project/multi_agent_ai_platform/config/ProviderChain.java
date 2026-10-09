package com.project.multi_agent_ai_platform.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * The chat model every agent talks to. It tries each provider that has a key, in
 * {@code AI_PROVIDERS} order, and returns the first answer; when one fails (bad key, rate limit,
 * outage), the next one is tried. Agents, memory and attachments know nothing about it: it sits
 * behind the same {@code ChatClient} they already use.
 * <p>
 * Every answer is labelled with the provider that gave it ({@link #PROVIDER}), and with the
 * providers that failed before it ({@link #FAILED_OVER}), so the console's Inspector can show both.
 */
public class ProviderChain implements ChatModel {

	/** Response metadata key: display name of the provider that answered. */
	public static final String PROVIDER = "provider";

	/** Response metadata key: one summary per provider that failed before the answer. */
	public static final String FAILED_OVER = "failedOver";

	private static final Logger log = LoggerFactory.getLogger(ProviderChain.class);

	/**
	 * One provider in the chain.
	 *
	 * @param provider what the console shows about it
	 * @param model    the model to call; {@code null} when it has no key (listed, never called)
	 */
	public record Link(LlmProvider provider, ChatModel model) {

		public boolean active() {
			return model != null && provider.apiKeyConfigured();
		}
	}

	private final List<Link> links;

	public ProviderChain(List<Link> links) {
		if (links.isEmpty()) {
			throw new IllegalArgumentException("The provider chain needs at least one provider");
		}
		this.links = List.copyOf(links);
	}

	/** Every provider in failover order, including the ones skipped for lack of a key. */
	public List<LlmProvider> providers() {
		return links.stream().map(Link::provider).toList();
	}

	/** The provider tried first: the first with a key, or the first listed when none has one. */
	public LlmProvider primary() {
		return links.stream().filter(Link::active).findFirst().orElse(links.getFirst()).provider();
	}

	/** The providers that will actually be called, in order. */
	public List<LlmProvider> active() {
		return links.stream().filter(Link::active).map(Link::provider).toList();
	}

	public boolean anyActive() {
		return links.stream().anyMatch(Link::active);
	}

	@Override
	public ChatResponse call(Prompt prompt) {
		// Each provider applies its own defaults (model, temperature): options built for one
		// provider's API mean nothing to another's.
		Prompt plain = new Prompt(prompt.getInstructions());
		List<ProviderFailure> failures = new ArrayList<>();
		for (Link link : links) {
			if (!link.active()) {
				continue;
			}
			try {
				ChatResponse response = link.model().call(plain);
				if (!failures.isEmpty()) {
					log.info("Answered by {} after {} provider(s) failed", link.provider().displayName(), failures.size());
				}
				return label(response, link.provider(), failures);
			}
			catch (RuntimeException ex) {
				ProviderFailure failure = ProviderFailure.of(link.provider(), ex);
				log.warn("AI provider failed: {}", failure.summary());
				failures.add(failure);
			}
		}
		throw new ProviderChainException(failures);
	}

	/** The same response, with the provider that answered and any providers that failed first. */
	private static ChatResponse label(ChatResponse response, LlmProvider provider, List<ProviderFailure> failures) {
		ChatResponseMetadata original = response.getMetadata();
		ChatResponseMetadata.Builder metadata = ChatResponseMetadata.builder();
		String model = provider.model();
		if (original != null) {
			for (Map.Entry<String, Object> entry : original.entrySet()) {
				metadata.keyValue(entry.getKey(), entry.getValue());
			}
			if (original.getId() != null) {
				metadata.id(original.getId());
			}
			if (original.getUsage() != null) {
				metadata.usage(original.getUsage());
			}
			if (original.getRateLimit() != null) {
				metadata.rateLimit(original.getRateLimit());
			}
			if (original.getPromptMetadata() != null) {
				metadata.promptMetadata(original.getPromptMetadata());
			}
			if (original.getModel() != null && !original.getModel().isBlank()) {
				model = original.getModel();
			}
		}
		metadata.model(model);
		metadata.keyValue(PROVIDER, provider.displayName());
		if (!failures.isEmpty()) {
			metadata.keyValue(FAILED_OVER, failures.stream().map(ProviderFailure::summary).toList());
		}
		return ChatResponse.builder().generations(response.getResults()).metadata(metadata.build()).build();
	}
}
