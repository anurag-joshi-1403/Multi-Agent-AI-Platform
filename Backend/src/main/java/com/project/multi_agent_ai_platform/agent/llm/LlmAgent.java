package com.project.multi_agent_ai_platform.agent.llm;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import com.project.multi_agent_ai_platform.agent.core.Agent;
import com.project.multi_agent_ai_platform.agent.core.AgentRequest;
import com.project.multi_agent_ai_platform.config.LlmProvider;

/**
 * Base class for agents that answer by calling the chat model.
 * <p>
 * Each subclass owns one {@link ChatClient} pre-loaded with its system prompt, and gets back the
 * model's text plus the facts the console's Inspector shows: provider, model, token usage and
 * finish reason.
 */
public abstract class LlmAgent implements Agent {

	private final ChatClient chatClient;

	private final LlmProvider provider;

	protected LlmAgent(ChatClient.Builder chatClientBuilder, LlmProvider provider, String systemPrompt) {
		// clone(): the builder may be shared, and defaultSystem would otherwise leak into other agents
		this.chatClient = chatClientBuilder.clone().defaultSystem(systemPrompt).build();
		this.provider = provider;
	}

	/** What the model said plus provider facts. */
	protected record Completion(String content, Map<String, Object> metadata) {
	}

	/** Send {@code userMessage} with the agent's system prompt. Provider errors propagate. */
	protected final Completion complete(AgentRequest request, String userMessage) {
		ChatResponse response = chatClient.prompt().user(userMessage).call().chatResponse();
		return toCompletion(response, request);
	}

	private Completion toCompletion(ChatResponse response, AgentRequest request) {
		// AgentResponse copies this with Map.copyOf, which rejects null values: only add what is present.
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("provider", provider.displayName());
		metadata.put("model", provider.model());
		if (request.conversationId() != null) {
			metadata.put("conversationId", request.conversationId());
		}

		String content = "";
		if (response != null) {
			Generation generation = response.getResult();
			if (generation != null && generation.getOutput() != null && generation.getOutput().getText() != null) {
				content = generation.getOutput().getText();
			}
			if (generation != null && generation.getMetadata() != null
					&& generation.getMetadata().getFinishReason() != null) {
				metadata.put("finishReason", generation.getMetadata().getFinishReason());
			}
			ChatResponseMetadata meta = response.getMetadata();
			if (meta != null) {
				// the model that actually answered, when the provider reports one
				if (meta.getModel() != null && !meta.getModel().isBlank()) {
					metadata.put("model", meta.getModel());
				}
				Usage usage = meta.getUsage();
				if (usage != null && usage.getTotalTokens() != null && usage.getTotalTokens() > 0) {
					metadata.put("tokens", Map.of(
							"prompt", usage.getPromptTokens(),
							"completion", usage.getCompletionTokens(),
							"total", usage.getTotalTokens()));
				}
			}
		}
		return new Completion(content, metadata);
	}
}
