package com.project.multi_agent_ai_platform.agent.llm;

import java.util.List;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

/**
 * Rewinds a conversation's memory for edit-and-resend: when the person edits an earlier message,
 * the turns from that message on are forgotten, so the agent answers the edited text without
 * seeing what came after it.
 * <p>
 * A turn is a user message plus everything after it up to the next user message (the reply).
 * Turns are counted from the end, which stays right after the memory window has already dropped
 * the oldest messages.
 */
public final class ConversationRewind {

	private ConversationRewind() {
	}

	/**
	 * {@code messages} without its last {@code turns} turns. Empty when it holds no more than that,
	 * including a reply left at the start by the memory window, whose question is already gone.
	 */
	public static List<Message> withoutLastTurns(List<Message> messages, int turns) {
		if (turns < 1) {
			return messages;
		}
		int seen = 0;
		for (int i = messages.size() - 1; i >= 0; i--) {
			if (messages.get(i).getMessageType() == MessageType.USER && ++seen == turns) {
				return List.copyOf(messages.subList(0, i));
			}
		}
		return List.of();
	}

	/** Forgets the last {@code turns} turns of {@code conversationId}; an unknown id is left alone. */
	public static void rewind(ChatMemory memory, String conversationId, int turns) {
		List<Message> messages = memory.get(conversationId);
		List<Message> kept = withoutLastTurns(messages, turns);
		if (kept.size() == messages.size()) {
			return;
		}
		memory.clear(conversationId);
		if (!kept.isEmpty()) {
			memory.add(conversationId, kept);
		}
	}
}
