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
import com.project.multi_agent_ai_platform.agent.core.AgentResponse;
import com.project.multi_agent_ai_platform.config.ProviderChain;
import com.project.multi_agent_ai_platform.document.AttachmentResolver;
import com.project.multi_agent_ai_platform.document.StoredDocument;

/**
 * Base class for agents that answer by calling the chat model.
 * <p>
 * Each subclass owns one {@link ChatClient} pre-loaded with its system prompt, and gets back the
 * model's text plus the facts the console's Inspector shows: which provider answered (and which
 * failed first), model, token usage and finish reason. The {@link ProviderChain} behind the
 * {@code ChatClient} decides which provider that is.
 * <p>
 * Conversation memory is attached per call, and only when the request carries a
 * {@code conversationId}, so single-shot calls never leak into a shared default conversation.
 * <p>
 * Files the caller attached are appended to the system prompt of every call, so any agent can
 * answer about them, and their text never enters conversation memory.
 * <p>
 * A subclass says what to send ({@link #turn}) and what to note about the finished answer
 * ({@link #describe}); this class runs the call ({@link #handle}).
 */
public abstract class LlmAgent implements Agent {

	private final ChatClient chatClient;

	private final ChatMemory chatMemory;

	private final MessageChatMemoryAdvisor memoryAdvisor;

	private final AttachmentResolver attachments;

	private final String systemPrompt;

	protected LlmAgent(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, AttachmentResolver attachments,
			String systemPrompt) {
		// clone(): the builder may be shared, and defaultSystem would otherwise leak into other agents
		this.chatClient = chatClientBuilder.clone().defaultSystem(systemPrompt).build();
		this.chatMemory = chatMemory;
		this.memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
		this.attachments = attachments;
		this.systemPrompt = systemPrompt;
	}

	/** Files attached to this request, in request order; ids the store no longer holds are skipped. */
	protected final List<StoredDocument> attached(AgentRequest request) {
		return attachments.resolve(request);
	}

	/**
	 * What to send for one request: the user message, the files attached to it, and any facts known
	 * before the answer (they go into its metadata).
	 */
	protected record Turn(String message, List<StoredDocument> attached, Map<String, ?> facts) {

		/** Public, unlike the record: agents in other packages create their turns. */
		public Turn {
			attached = List.copyOf(attached);
			facts = Map.copyOf(facts);
		}

		public Turn(String message, List<StoredDocument> attached) {
			this(message, attached, Map.of());
		}
	}

	/** What to send for this request. Throws for a request this agent can't handle, before any call. */
	protected abstract Turn turn(AgentRequest request);

	/** Notes about the finished answer for its metadata, e.g. the code's language. None by default. */
	protected void describe(String content, Map<String, Object> metadata) {
	}

	/**
	 * Sends the {@link #turn} with the agent's system prompt plus the attached files, replaying the
	 * conversation's earlier turns when the request has a {@code conversationId}. Provider errors
	 * propagate.
	 */
	@Override
	public final AgentResponse handle(AgentRequest request) {
		Turn turn = turn(request);
		try {
			ChatResponse response = prompt(request, turn).call().chatResponse();
			return finish(request, turn, textOf(response), response);
		}
		catch (RuntimeException ex) {
			// The memory advisor stores the user turn before the model is called. If the call fails,
			// drop that turn so the next request does not replay an unanswered question.
			forgetDanglingUserTurn(request);
			throw ex;
		}
	}

	private ChatClient.ChatClientRequestSpec prompt(AgentRequest request, Turn turn) {
		ChatClient.ChatClientRequestSpec spec = chatClient.prompt();
		String fileBlock = attachments.block(turn.attached());
		if (fileBlock != null) {
			spec = spec.system(systemPrompt + fileBlock);
		}
		String conversationId = conversationId(request);
		if (conversationId != null) {
			spec = spec.advisors(memoryAdvisor).advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId));
		}
		return spec.user(turn.message());
	}

	private AgentResponse finish(AgentRequest request, Turn turn, String content, ChatResponse response) {
		Map<String, Object> metadata = metadataOf(response, request);
		if (!turn.attached().isEmpty()) {
			metadata.put("documentNames", turn.attached().stream().map(StoredDocument::name).toList());
		}
		metadata.putAll(turn.facts());
		describe(content, metadata);
		return new AgentResponse(id(), content, metadata);
	}

	private void forgetDanglingUserTurn(AgentRequest request) {
		String conversationId = conversationId(request);
		if (conversationId == null) {
			return;
		}
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

	/** The answer's text, or {@code ""} for a response without any. */
	private static String textOf(ChatResponse response) {
		Generation generation = response == null ? null : response.getResult();
		if (generation == null || generation.getOutput() == null || generation.getOutput().getText() == null) {
			return "";
		}
		return generation.getOutput().getText();
	}

	/** Provider facts for the Inspector. */
	private static Map<String, Object> metadataOf(ChatResponse response, AgentRequest request) {
		// AgentResponse copies this with Map.copyOf, which rejects null values: only add what is present.
		Map<String, Object> metadata = new LinkedHashMap<>();
		if (request.conversationId() != null) {
			metadata.put("conversationId", request.conversationId());
		}

		if (response != null) {
			Generation generation = response.getResult();
			if (generation != null && generation.getMetadata() != null
					&& generation.getMetadata().getFinishReason() != null) {
				metadata.put("finishReason", generation.getMetadata().getFinishReason());
			}
			ChatResponseMetadata meta = response.getMetadata();
			if (meta != null) {
				if (meta.get(ProviderChain.PROVIDER) instanceof String provider) {
					metadata.put("provider", provider);
				}
				if (meta.getModel() != null && !meta.getModel().isBlank()) {
					metadata.put("model", meta.getModel());
				}
				if (meta.get(ProviderChain.FAILED_OVER) instanceof List<?> failedOver && !failedOver.isEmpty()) {
					metadata.put("failedOver", failedOver);
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
		return metadata;
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
