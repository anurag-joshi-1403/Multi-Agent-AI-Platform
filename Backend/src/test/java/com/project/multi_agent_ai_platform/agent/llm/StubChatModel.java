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

/**
 * A {@link ChatModel} for tests: records every prompt it receives and answers with a canned
 * reply plus realistic metadata (model name, token usage, finish reason).
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
		return prompts.get(prompts.size() - 1);
	}

	/** A fresh builder wired to this stub, as the agents would receive from Spring. */
	public ChatClient.Builder clientBuilder() {
		return ChatClient.builder(this);
	}

	/** Fresh in-memory conversation memory, as {@code AiConfig} would provide. */
	public static ChatMemory memory() {
		return MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.maxMessages(20)
			.build();
	}
}
