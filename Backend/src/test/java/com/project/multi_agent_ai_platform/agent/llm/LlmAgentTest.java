package com.project.multi_agent_ai_platform.agent.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import com.project.multi_agent_ai_platform.agent.core.AgentRequest;
import com.project.multi_agent_ai_platform.agent.core.AgentResponse;

class LlmAgentTest {

	private final StubChatModel model = new StubChatModel();

	/** Smallest possible concrete agent for exercising the base class. */
	private static LlmAgent agent(StubChatModel model, String systemPrompt) {
		return new LlmAgent(model.clientBuilder(), StubChatModel.provider(), systemPrompt) {
			@Override
			public String id() {
				return "test";
			}

			@Override
			public String description() {
				return "test agent";
			}

			@Override
			public AgentResponse handle(AgentRequest request) {
				Completion completion = complete(request, request.message());
				return new AgentResponse(id(), completion.content(), completion.metadata());
			}
		};
	}

	@Test
	void sendsSystemPromptAndMessageAndReturnsProviderMetadata() {
		model.reply = "hello back";

		AgentResponse response = agent(model, "You are a test.").handle(new AgentRequest("c-1", "hi", Map.of()));

		assertThat(response.content()).isEqualTo("hello back");
		assertThat(response.metadata())
			.containsEntry("provider", "Google Gemini")
			.containsEntry("model", "stub-model")
			.containsEntry("finishReason", "STOP")
			.containsEntry("conversationId", "c-1")
			.containsEntry("tokens", Map.of("prompt", 10, "completion", 5, "total", 15));
		assertThat(model.lastPrompt().getSystemMessage().getText()).isEqualTo("You are a test.");
		assertThat(model.lastPrompt().getUserMessage().getText()).isEqualTo("hi");
	}

	@Test
	void callsCarryNoHistoryUntilMemoryArrives() {
		LlmAgent agent = agent(model, "sys");
		agent.handle(new AgentRequest("c-1", "first", Map.of()));
		agent.handle(new AgentRequest("c-1", "second", Map.of()));

		List<Message> instructions = model.lastPrompt().getInstructions();
		assertThat(instructions).extracting(Message::getMessageType)
			.containsExactly(MessageType.SYSTEM, MessageType.USER);
	}

	@Test
	void fallsBackToConfiguredModelWhenProviderReportsNone() {
		StubChatModel silent = new StubChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				ChatResponse full = super.call(prompt);
				return ChatResponse.builder()
					.generations(full.getResults())
					.metadata(ChatResponseMetadata.builder().build())
					.build();
			}
		};

		AgentResponse response = agent(silent, "sys").handle(AgentRequest.of("hi"));

		assertThat(response.metadata())
			.containsEntry("model", "gemini-test")
			.doesNotContainKey("tokens")
			.doesNotContainKey("conversationId");
	}

	@Test
	void providerErrorsPropagate() {
		StubChatModel failing = new StubChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				throw new IllegalStateException("provider down");
			}
		};

		assertThatThrownBy(() -> agent(failing, "sys").handle(AgentRequest.of("hi")))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("provider down");
	}
}
