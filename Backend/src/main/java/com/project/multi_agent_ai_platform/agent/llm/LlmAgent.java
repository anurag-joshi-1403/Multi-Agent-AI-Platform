package com.project.multi_agent_ai_platform.agent.llm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import com.project.multi_agent_ai_platform.agent.core.Agent;
import com.project.multi_agent_ai_platform.agent.core.AgentRequest;
import com.project.multi_agent_ai_platform.config.LlmProvider;
import com.project.multi_agent_ai_platform.document.AttachmentResolver;
import com.project.multi_agent_ai_platform.document.StoredDocument;

/**
 * Base class for agents that answer by calling the chat model.
 * <p>
 * Each subclass owns one {@link ChatClient} pre-loaded with its system prompt, and gets back the
 * model's text plus the facts the console's Inspector shows: provider, model, token usage and
 * finish reason.
 * <p>
 * Conversation memory is attached per call, and only when the request carries a
 * {@code conversationId}, so single-shot calls never leak into a shared default conversation.
 * <p>
 * Files the caller attached are appended to the system prompt of every call, so any agent can
 * answer about them, and their text never enters conversation memory.
 */
public abstract class LlmAgent implements Agent {

	private final ChatClient chatClient;

	private final ChatMemory chatMemory;

	private final MessageChatMemoryAdvisor memoryAdvisor;

	private final AttachmentResolver attachments;

	private final LlmProvider provider;

	private final String systemPrompt;

	protected LlmAgent(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, AttachmentResolver attachments,
			LlmProvider provider, String systemPrompt) {
		// clone(): the builder may be shared, and defaultSystem would otherwise leak into other agents
		this.chatClient = chatClientBuilder.clone().defaultSystem(systemPrompt).build();
		this.chatMemory = chatMemory;
		this.memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
		this.attachments = attachments;
		this.provider = provider;
		this.systemPrompt = systemPrompt;
	}

	/** Files attached to this request, in request order; ids the store no longer holds are skipped. */
	protected final List<StoredDocument> attached(AgentRequest request) {
		return attachments.resolve(request);
	}

	/** What the model said plus provider facts. */
	protected record Completion(String content, Map<String, Object> metadata) {
	}

	/**
	 * Send {@code userMessage} with the agent's system prompt plus the request's attached files,
	 * replaying the conversation's earlier turns when the request has a {@code conversationId}.
	 * Provider errors propagate.
	 */
	protected final Completion complete(AgentRequest request, String userMessage) {
		return complete(request, userMessage, attached(request));
	}

	/** As {@link #complete(AgentRequest, String)}, for agents that already resolved the attachments. */
	protected final Completion complete(AgentRequest request, String userMessage, List<StoredDocument> attached) {
		ChatClient.ChatClientRequestSpec spec = chatClient.prompt();
		String fileBlock = attachments.block(attached);
		if (fileBlock != null) {
			spec = spec.system(systemPrompt + fileBlock);
		}
		String conversationId = conversationId(request);
		if (conversationId != null) {
			spec = spec.advisors(memoryAdvisor).advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId));
		}
		try {
			ChatResponse response = spec.user(userMessage).call().chatResponse();
			Completion completion = toCompletion(response, request);
			if (!attached.isEmpty()) {
				completion.metadata().put("documentNames", attached.stream().map(StoredDocument::name).toList());
			}
			return completion;
		}
		catch (RuntimeException ex) {
			// The memory advisor stores the user turn before the model is called. If the call fails,
			// drop that turn so the next request does not replay an unanswered question.
			if (conversationId != null) {
				forgetDanglingUserTurn(conversationId);
			}
			throw ex;
		}
	}

	private void forgetDanglingUserTurn(String conversationId) {
		List<Message> messages = chatMemory.get(conversationId);
		if (!messages.isEmpty() && messages.getLast().getMessageType() == MessageType.USER) {
			List<Message> kept = List.copyOf(messages.subList(0, messages.size() - 1));
			chatMemory.clear(conversationId);
			if (!kept.isEmpty()) {
				chatMemory.add(conversationId, kept);
			}
		}
	}

	private static String conversationId(AgentRequest request) {
		String id = request.conversationId();
		return id == null || id.isBlank() ? null : id;
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

	/** Attribute as a trimmed string, or {@code null} when absent or blank. */
	protected static String attribute(AgentRequest request, String key) {
		Object value = request.attributes().get(key);
		if (value == null) {
			return null;
		}
		String text = String.valueOf(value).trim();
		return text.isEmpty() ? null : text;
	}

	/** Attribute as an int, or {@code fallback} when absent or not numeric. */
	protected static int attribute(AgentRequest request, String key, int fallback) {
		Object value = request.attributes().get(key);
		if (value instanceof Number n) {
			return n.intValue();
		}
		if (value instanceof String s) {
			try {
				return Integer.parseInt(s.trim());
			}
			catch (NumberFormatException ignored) {
				// fall through
			}
		}
		return fallback;
	}
}
