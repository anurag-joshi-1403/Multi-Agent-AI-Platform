package com.project.multi_agent_ai_platform.web;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Server-side conversation memory. The console keeps its own transcript in the browser; this lets
 * it make the server forget a conversation too, when the chat is deleted.
 * <pre>
 *   DELETE /api/conversations/{id}   forget the conversation (idempotent, 204)
 * </pre>
 */
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

	private final ChatMemory chatMemory;

	public ConversationController(ChatMemory chatMemory) {
		this.chatMemory = chatMemory;
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> forget(@PathVariable String id) {
		chatMemory.clear(id);
		return ResponseEntity.noContent().build();
	}
}
