package com.project.multi_agent_ai_platform.agent.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import com.project.multi_agent_ai_platform.agent.core.AgentRequest;
import com.project.multi_agent_ai_platform.agent.core.AgentResponse;
import com.project.multi_agent_ai_platform.config.LlmProvider;
import com.project.multi_agent_ai_platform.config.ProviderChain;
import com.project.multi_agent_ai_platform.document.FakeDocumentStore;
import com.project.multi_agent_ai_platform.document.StoredDocument;

class LlmAgentTest {

	private final StubChatModel model = new StubChatModel();

	private final ChatMemory memory = StubChatModel.memory();

	private final FakeDocumentStore store = new FakeDocumentStore();

	/** Smallest possible concrete agent for exercising the base class. */
	private LlmAgent agent(ChatModel model, ChatMemory memory, String systemPrompt) {
		return new LlmAgent(ChatClient.builder(model), memory, StubChatModel.attachments(store), systemPrompt) {
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

	private LlmAgent agent() {
		return agent(model, memory, "sys");
	}

	private static List<MessageType> types(Prompt prompt) {
		return prompt.getInstructions().stream().map(Message::getMessageType).toList();
	}

	// --- one call ----------------------------------------------------------------------------

	@Test
	void sendsSystemPromptAndMessageAndReturnsProviderMetadata() {
		model.reply = "hello back";
		ProviderChain chain = new ProviderChain(List.of(new ProviderChain.Link(StubChatModel.provider(), model)));

		AgentResponse response = agent(chain, memory, "You are a test.").handle(new AgentRequest("c-1", "hi", Map.of()));

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
	void reportsTheProvidersThatFailedBeforeTheAnswer() {
		StubChatModel down = new StubChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				throw new IllegalStateException("connection reset");
			}
		};
		LlmProvider groq = LlmProvider.of("groq", "Groq", "GROQ_API_KEY", "llama", "k");
		ProviderChain chain = new ProviderChain(
				List.of(new ProviderChain.Link(groq, down), new ProviderChain.Link(StubChatModel.provider(), model)));

		AgentResponse response = agent(chain, memory, "sys").handle(AgentRequest.of("hi"));

		assertThat(response.metadata())
			.containsEntry("provider", "Google Gemini")
			.containsEntry("failedOver", List.of("Groq failed: connection reset"));
	}

	@Test
	void inventsNothingTheProviderDidNotReport() {
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

		AgentResponse response = agent(silent, memory, "sys").handle(AgentRequest.of("hi"));

		assertThat(response.metadata())
			.doesNotContainKeys("provider", "model", "tokens", "failedOver", "conversationId");
	}

	// --- memory ------------------------------------------------------------------------------

	@Test
	void conversationIdReplaysEarlierTurns() {
		LlmAgent agent = agent();
		model.reply = "answer one";
		agent.handle(new AgentRequest("conv-1", "first", Map.of()));
		model.reply = "answer two";
		agent.handle(new AgentRequest("conv-1", "second", Map.of()));

		assertThat(types(model.lastPrompt()))
			.containsExactly(MessageType.SYSTEM, MessageType.USER, MessageType.ASSISTANT, MessageType.USER);
		assertThat(model.lastPrompt().getInstructions()).extracting(Message::getText)
			.contains("first", "answer one", "second");
		assertThat(memory.get("conv-1")).hasSize(4);
	}

	@Test
	void missingOrBlankConversationIdCarriesNoHistory() {
		// Spring AI's memory rejects blank ids outright, so they must skip memory rather than reach it
		LlmAgent agent = agent();
		agent.handle(AgentRequest.of("first"));
		agent.handle(new AgentRequest("  ", "second", Map.of()));
		agent.handle(new AgentRequest("  ", "third", Map.of()));

		assertThat(types(model.lastPrompt())).containsExactly(MessageType.SYSTEM, MessageType.USER);
	}

	@Test
	void differentConversationsDoNotShareMemory() {
		LlmAgent agent = agent();
		agent.handle(new AgentRequest("conv-a", "hello from a", Map.of()));
		agent.handle(new AgentRequest("conv-b", "hello from b", Map.of()));

		assertThat(model.lastPrompt().getInstructions()).extracting(Message::getText).doesNotContain("hello from a");
	}

	@Test
	void agentsShareMemoryWithinAConversation() {
		LlmAgent one = agent();
		LlmAgent two = agent(model, memory, "other system prompt");
		one.handle(new AgentRequest("conv-1", "asked agent one", Map.of()));
		two.handle(new AgentRequest("conv-1", "asked agent two", Map.of()));

		assertThat(model.lastPrompt().getSystemMessage().getText()).isEqualTo("other system prompt");
		assertThat(model.lastPrompt().getInstructions()).extracting(Message::getText).contains("asked agent one");
	}

	@Test
	void failedProviderCallDoesNotLeaveADanglingUserTurn() {
		StubChatModel failing = new StubChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				if (prompt.getUserMessage().getText().equals("boom")) {
					throw new IllegalStateException("provider down");
				}
				return super.call(prompt);
			}
		};
		LlmAgent flaky = agent(failing, memory, "sys");

		flaky.handle(new AgentRequest("conv-x", "fine", Map.of()));
		assertThatThrownBy(() -> flaky.handle(new AgentRequest("conv-x", "boom", Map.of())))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("provider down");

		assertThat(memory.get("conv-x")).extracting(Message::getText).containsExactly("fine", "stub reply");
	}

	@Test
	void failedFirstCallLeavesTheConversationEmpty() {
		StubChatModel failing = new StubChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				throw new IllegalStateException("provider down");
			}
		};

		assertThatThrownBy(() -> agent(failing, memory, "sys").handle(new AgentRequest("conv-y", "hi", Map.of())))
			.isInstanceOf(IllegalStateException.class);
		assertThat(memory.get("conv-y")).isEmpty();
	}

	// --- attached files ----------------------------------------------------------------------

	private static AgentRequest withFiles(String conversationId, String message, StoredDocument... docs) {
		return new AgentRequest(conversationId, message,
				Map.of("attachments", java.util.Arrays.stream(docs).map(StoredDocument::id).toList()));
	}

	@Test
	void attachedFilesRideInTheSystemPromptAndAreNamedInTheMetadata() {
		StoredDocument doc = store.save("notes.txt", "text/plain", "The launch is on 3 March.", null);

		AgentResponse response = agent().handle(withFiles(null, "When is the launch?", doc));

		assertThat(model.lastPrompt().getSystemMessage().getText())
			.startsWith("sys")
			.contains("--- FILE: notes.txt ---")
			.contains("The launch is on 3 March.");
		assertThat(model.lastPrompt().getUserMessage().getText()).isEqualTo("When is the launch?");
		assertThat(response.metadata()).containsEntry("documentNames", List.of("notes.txt"));
	}

	@Test
	void fileTextNeverEntersConversationMemory() {
		StoredDocument doc = store.save("secret-plan.txt", "text/plain", "TOP SECRET CONTENT", null);

		agent().handle(withFiles("conv-f", "summarise it", doc));

		assertThat(memory.get("conv-f")).extracting(Message::getText)
			.containsExactly("summarise it", "stub reply")
			.noneMatch(text -> text.contains("TOP SECRET CONTENT"));
	}

	@Test
	void withoutFilesTheSystemPromptIsUnchangedAndNoNamesAreReported() {
		AgentResponse response = agent().handle(withFiles(null, "hi"));

		assertThat(model.lastPrompt().getSystemMessage().getText()).isEqualTo("sys");
		assertThat(response.metadata()).doesNotContainKey("documentNames");
	}

	// --- attribute helpers -------------------------------------------------------------------

	@Test
	void attributeHelpers() {
		AgentRequest request = new AgentRequest(null, "x", Map.of("s", " v ", "blank", "  ", "n", 7, "t", "12", "bad", "abc"));

		assertThat(LlmAgent.attribute(request, "s")).isEqualTo("v");
		assertThat(LlmAgent.attribute(request, "blank")).isNull();
		assertThat(LlmAgent.attribute(request, "missing")).isNull();
		assertThat(LlmAgent.attribute(request, "n", 1)).isEqualTo(7);
		assertThat(LlmAgent.attribute(request, "t", 1)).isEqualTo(12);
		assertThat(LlmAgent.attribute(request, "bad", 1)).isEqualTo(1);
		assertThat(LlmAgent.attribute(request, "missing", 1)).isEqualTo(1);
	}
}
