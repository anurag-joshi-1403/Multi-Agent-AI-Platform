package com.project.multi_agent_ai_platform.web;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Server-side conversation memory. The frontend keeps its own transcript; these endpoints expose
 * what the model actually gets replayed and let a client start over.
 */
@RestController
@RequestMapping(path = "/api/conversations", produces = MediaType.APPLICATION_JSON_VALUE)
public class ConversationController {

	private final ChatMemory chatMemory;

	public ConversationController(ChatMemory chatMemory) {
		this.chatMemory = chatMemory;
	}

	/** The messages currently held in the memory window for this conversation. */
	@GetMapping("/{id}/memory")
	public List<Map<String, Object>> memory(@PathVariable String id) {
		return chatMemory.get(id).stream().map(ConversationController::toJson).toList();
	}

	/** Forget the conversation on the server side. Idempotent. */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> forget(@PathVariable String id) {
		chatMemory.clear(id);
		return ResponseEntity.noContent().build();
	}

	private static Map<String, Object> toJson(Message message) {
		String text = message.getText() == null ? "" : message.getText();
		return Map.of("role", message.getMessageType().getValue(), "text", text);
	}
}
