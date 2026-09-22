package com.project.multi_agent_ai_platform.agent.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import com.project.multi_agent_ai_platform.agent.core.AgentRequest;
import com.project.multi_agent_ai_platform.agent.core.AgentResponse;
import com.project.multi_agent_ai_platform.config.PlatformProperties;
import com.project.multi_agent_ai_platform.document.AttachmentResolver;
import com.project.multi_agent_ai_platform.document.InMemoryDocumentStore;

class LlmAgentTest {

	private final StubChatModel model = new StubChatModel();

	private static AttachmentResolver resolver() {
		PlatformProperties properties = new PlatformProperties(PlatformProperties.Storage.MEMORY, new PlatformProperties.Cors(List.of()),
				new PlatformProperties.Memory(20), new PlatformProperties.Documents(60_000, 50),
				new PlatformProperties.Auth("tester:tester"));
		return new AttachmentResolver(new InMemoryDocumentStore(properties), properties);
	}

	/** Smallest possible concrete agent for exercising the base class. */
	private final LlmAgent agent = new LlmAgent(model.clientBuilder(), StubChatModel.memory(), resolver(),
			"You are a test.") {
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

	@Test
	void usesSystemPromptAndReturnsProviderMetadata() {
		model.reply = "hello back";

		AgentResponse response = agent.handle(AgentRequest.of("hi"));

		assertThat(response.content()).isEqualTo("hello back");
		assertThat(response.metadata())
			.containsEntry("model", "stub-model")
			.containsEntry("finishReason", "STOP")
			.containsEntry("tokens", Map.of("prompt", 10, "completion", 5, "total", 15));
		assertThat(model.lastPrompt().getSystemMessage().getText()).isEqualTo("You are a test.");
		assertThat(model.lastPrompt().getUserMessage().getText()).isEqualTo("hi");
	}

	@Test
	void singleShotCallsCarryNoHistory() {
		agent.handle(AgentRequest.of("first"));
		agent.handle(AgentRequest.of("second"));

		List<Message> instructions = model.lastPrompt().getInstructions();
		assertThat(instructions).extracting(Message::getMessageType)
			.containsExactly(MessageType.SYSTEM, MessageType.USER);
	}

	@Test
	void conversationIdReplaysEarlierTurns() {
		model.reply = "answer one";
		agent.handle(new AgentRequest("conv-1", "first", Map.of()));
		model.reply = "answer two";
		agent.handle(new AgentRequest("conv-1", "second", Map.of()));

		List<Message> instructions = model.lastPrompt().getInstructions();
		assertThat(instructions).extracting(Message::getMessageType)
			.containsExactly(MessageType.SYSTEM, MessageType.USER, MessageType.ASSISTANT, MessageType.USER);
		assertThat(instructions).extracting(Message::getText).contains("first", "answer one", "second");
	}

	@Test
	void differentConversationsDoNotShareMemory() {
		agent.handle(new AgentRequest("conv-a", "hello from a", Map.of()));
		agent.handle(new AgentRequest("conv-b", "hello from b", Map.of()));

		assertThat(model.lastPrompt().getInstructions()).extracting(Message::getText)
			.doesNotContain("hello from a");
	}

	@Test
	void failedProviderCallDoesNotLeaveDanglingUserTurnInMemory() {
		ChatMemory memory = StubChatModel.memory();
		StubChatModel failing = new StubChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				if (prompt.getUserMessage().getText().equals("boom")) {
					throw new IllegalStateException("provider down");
				}
				return super.call(prompt);
			}
		};
		LlmAgent flaky = new LlmAgent(failing.clientBuilder(), memory, resolver(), "sys") {
			@Override
			public String id() {
				return "flaky";
			}

			@Override
			public String description() {
				return "flaky";
			}

			@Override
			public AgentResponse handle(AgentRequest request) {
				Completion completion = complete(request, request.message());
				return new AgentResponse(id(), completion.content(), completion.metadata());
			}
		};

		flaky.handle(new AgentRequest("conv-x", "fine", Map.of()));
		assertThatThrownBy(() -> flaky.handle(new AgentRequest("conv-x", "boom", Map.of())))
			.isInstanceOf(IllegalStateException.class);

		assertThat(memory.get("conv-x")).extracting(Message::getText).containsExactly("fine", "stub reply");
	}

	@Test
	void attributeHelpers() {
		AgentRequest request = new AgentRequest(null, "x", Map.of("s", " v ", "n", 7, "t", "12", "bad", "abc"));

		assertThat(LlmAgent.attribute(request, "s")).isEqualTo("v");
		assertThat(LlmAgent.attribute(request, "missing")).isNull();
		assertThat(LlmAgent.attribute(request, "n", 1)).isEqualTo(7);
		assertThat(LlmAgent.attribute(request, "t", 1)).isEqualTo(12);
		assertThat(LlmAgent.attribute(request, "bad", 1)).isEqualTo(1);
		assertThat(LlmAgent.attribute(request, "missing", 1)).isEqualTo(1);
	}
}
