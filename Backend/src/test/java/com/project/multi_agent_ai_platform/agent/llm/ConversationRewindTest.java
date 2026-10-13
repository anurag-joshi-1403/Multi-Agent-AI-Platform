package com.project.multi_agent_ai_platform.agent.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

class ConversationRewindTest {

	private static final List<Message> THREE_TURNS = List.of(new UserMessage("q1"), new AssistantMessage("a1"),
			new UserMessage("q2"), new AssistantMessage("a2"), new UserMessage("q3"), new AssistantMessage("a3"));

	private static List<String> texts(List<Message> messages) {
		return messages.stream().map(Message::getText).toList();
	}

	@Test
	void dropsTheLastTurnsAndKeepsTheEarlierOnes() {
		assertThat(texts(ConversationRewind.withoutLastTurns(THREE_TURNS, 1))).containsExactly("q1", "a1", "q2", "a2");
		assertThat(texts(ConversationRewind.withoutLastTurns(THREE_TURNS, 2))).containsExactly("q1", "a1");
		assertThat(ConversationRewind.withoutLastTurns(THREE_TURNS, 3)).isEmpty();
	}

	@Test
	void moreTurnsThanStoredEmptiesTheConversation() {
		assertThat(ConversationRewind.withoutLastTurns(THREE_TURNS, 10)).isEmpty();
	}

	@Test
	void countsFromTheEndWhenTheWindowAlreadyDroppedTheStart() {
		// The 20-message window cut "q0", leaving its reply first
		List<Message> window = List.of(new AssistantMessage("a0"), new UserMessage("q1"), new AssistantMessage("a1"),
				new UserMessage("q2"), new AssistantMessage("a2"));

		assertThat(texts(ConversationRewind.withoutLastTurns(window, 1))).containsExactly("a0", "q1", "a1");
		assertThat(texts(ConversationRewind.withoutLastTurns(window, 2))).containsExactly("a0");
		// Editing a message older than the window: everything left came after it
		assertThat(ConversationRewind.withoutLastTurns(window, 3)).isEmpty();
	}

	@Test
	void noTurnsChangesNothing() {
		assertThat(ConversationRewind.withoutLastTurns(THREE_TURNS, 0)).isEqualTo(THREE_TURNS);
	}

	@Test
	void rewindsOneConversationAndLeavesOthersAlone() {
		ChatMemory memory = StubChatModel.memory();
		memory.add("c-1", THREE_TURNS);
		memory.add("c-2", List.of(new UserMessage("other"), new AssistantMessage("reply")));

		ConversationRewind.rewind(memory, "c-1", 2);

		assertThat(texts(memory.get("c-1"))).containsExactly("q1", "a1");
		assertThat(texts(memory.get("c-2"))).containsExactly("other", "reply");
	}

	@Test
	void rewindingEverythingOrAnUnknownConversationLeavesItEmpty() {
		ChatMemory memory = StubChatModel.memory();
		memory.add("c-1", THREE_TURNS);

		ConversationRewind.rewind(memory, "c-1", 3);
		ConversationRewind.rewind(memory, "never-existed", 1);

		assertThat(memory.get("c-1")).isEmpty();
		assertThat(memory.get("never-existed")).isEmpty();
	}
}
