package com.project.multi_agent_ai_platform.agent.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import com.project.multi_agent_ai_platform.agent.core.AgentRequest;
import com.project.multi_agent_ai_platform.agent.core.AgentResponse;
import com.project.multi_agent_ai_platform.agent.core.InvalidAgentRequestException;
import com.project.multi_agent_ai_platform.agent.llm.LlmAgent;
import com.project.multi_agent_ai_platform.config.LlmProvider;
import com.project.multi_agent_ai_platform.document.AttachmentResolver;
import com.project.multi_agent_ai_platform.document.StoredDocument;

/**
 * Answers questions strictly from the files attached to the conversation. Every agent can read
 * attachments (see {@link LlmAgent}); this one adds the guarantee that it answers only from them,
 * and refuses to run without one.
 */
@Component
public class DocumentAgent extends LlmAgent {

	static final String SYSTEM_PROMPT = """
			You answer questions strictly from the files attached to this conversation.
			- Quote the relevant passage briefly (with its page marker when present) before answering.
			- If the attached files do not contain the answer, say exactly that; do not guess from outside knowledge.
			- Be concise.
			""";

	public DocumentAgent(ChatClient.Builder builder, ChatMemory chatMemory, AttachmentResolver attachments,
			LlmProvider provider) {
		super(builder, chatMemory, attachments, provider, SYSTEM_PROMPT);
	}

	@Override
	public String id() {
		return "document";
	}

	@Override
	public String description() {
		return "Answers questions grounded in the PDF or text files you attach to the message.";
	}

	@Override
	public List<String> capabilities() {
		return List.of("PDF Q&A", "Extraction", "Grounded answers");
	}

	@Override
	public AgentResponse handle(AgentRequest request) {
		List<StoredDocument> attached = attached(request);
		if (attached.isEmpty()) {
			throw new InvalidAgentRequestException(
					"The Document Agent needs at least one attached file. Attach a PDF or text file to your message.");
		}

		Completion completion = complete(request, request.message(), attached);

		Map<String, Object> metadata = new LinkedHashMap<>(completion.metadata());
		metadata.put("documentIds", attached.stream().map(StoredDocument::id).toList());
		return new AgentResponse(id(), completion.content(), metadata);
	}
}
