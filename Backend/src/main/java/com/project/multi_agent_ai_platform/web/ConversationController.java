package com.project.multi_agent_ai_platform.web;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.multi_agent_ai_platform.agent.llm.ConversationRewind;
import com.project.multi_agent_ai_platform.web.dto.RewindRequest;

import jakarta.validation.Valid;

/**
 * Server-side conversation memory. The console keeps its own transcript in the browser; this lets
 * it make the server forget a conversation too, when the chat is deleted, or its latest turns,
 * when an earlier message is edited and sent again.
 * <pre>
 *   DELETE /api/conversations/{id}          forget the conversation (idempotent, 204)
 *   POST   /api/conversations/{id}/rewind   {turns} forget the last turns (204; 400 for turns < 1)
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

	@PostMapping(path = "/{id}/rewind", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> rewind(@PathVariable String id, @Valid @RequestBody RewindRequest body) {
		ConversationRewind.rewind(chatMemory, id, body.turns());
		return ResponseEntity.noContent().build();
	}
}
