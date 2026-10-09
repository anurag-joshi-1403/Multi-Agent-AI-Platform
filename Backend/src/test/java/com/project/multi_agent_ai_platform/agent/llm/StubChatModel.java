package com.project.multi_agent_ai_platform.agent.llm;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import com.project.multi_agent_ai_platform.config.LlmProvider;
import com.project.multi_agent_ai_platform.config.PlatformProperties;
import com.project.multi_agent_ai_platform.document.AttachmentResolver;
import com.project.multi_agent_ai_platform.document.DocumentStore;
import com.project.multi_agent_ai_platform.document.FakeDocumentStore;

/**
 * A {@link ChatModel} for tests: records every prompt it receives and answers with a canned reply
 * plus realistic metadata (model name, token usage, finish reason). No key, no network.
 */
public class StubChatModel implements ChatModel {

	public final List<Prompt> prompts = new ArrayList<>();

	public String reply = "stub reply";

	@Override
	public ChatResponse call(Prompt prompt) {
		prompts.add(prompt);
		ChatResponseMetadata metadata = ChatResponseMetadata.builder()
			.model("stub-model")
			.usage(new DefaultUsage(10, 5))
			.build();
		Generation generation = new Generation(new AssistantMessage(reply),
				ChatGenerationMetadata.builder().finishReason("STOP").build());
		return ChatResponse.builder().generations(List.of(generation)).metadata(metadata).build();
	}

	public Prompt lastPrompt() {
		return prompts.getLast();
	}

	/** A fresh builder wired to this stub, as the agents would receive from Spring. */
	public ChatClient.Builder clientBuilder() {
		return ChatClient.builder(this);
	}

	/** The provider description the agents would receive from {@code AiConfig}. */
	public static LlmProvider provider() {
		return LlmProvider.gemini("gemini-test", "test-key-not-used");
	}

	/** Fresh in-memory conversation memory, as {@code AiConfig} would provide. */
	public static ChatMemory memory() {
		return MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.maxMessages(20)
			.build();
	}

	/** The platform's default settings: 20 messages of memory, 60 000 chars of file context, 50 uploads. */
	public static PlatformProperties properties() {
		return new PlatformProperties(new PlatformProperties.Memory(20), new PlatformProperties.Documents(60_000, 50),
				new PlatformProperties.Auth(""));
	}

	/** An attachment resolver over {@code store}, with the default limits. */
	public static AttachmentResolver attachments(DocumentStore store) {
		return new AttachmentResolver(store, properties());
	}

	/** An attachment resolver over an empty store, for agents tested without files. */
	public static AttachmentResolver noAttachments() {
		return attachments(new FakeDocumentStore());
	}
}
