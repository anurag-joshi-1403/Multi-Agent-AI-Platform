package com.project.multi_agent_ai_platform.agent.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import com.project.multi_agent_ai_platform.agent.core.AgentParameter;
import com.project.multi_agent_ai_platform.agent.core.AgentRequest;
import com.project.multi_agent_ai_platform.agent.core.AgentResponse;
import com.project.multi_agent_ai_platform.agent.core.InvalidAgentRequestException;
import com.project.multi_agent_ai_platform.agent.llm.LlmAgent;
import com.project.multi_agent_ai_platform.document.AttachmentResolver;
import com.project.multi_agent_ai_platform.document.StoredDocument;

/**
 * Answers questions grounded in the files attached to the conversation. Attachments are uploaded
 * via {@code POST /api/documents} and their ids passed in the {@code attachments} attribute, which
 * {@link LlmAgent} folds into the per-call system prompt. This agent adds the guarantee that it
 * refuses to answer without one.
 */
@Component
public class DocumentAgent extends LlmAgent {

	static final String SYSTEM_PROMPT = """
			You answer questions strictly from the files attached to this conversation.
			- Quote the relevant passage briefly (with its page marker when present) before answering.
			- If the attached files do not contain the answer, say exactly that; do not guess from outside knowledge.
			- Be concise.
			""";

	public DocumentAgent(ChatClient.Builder builder, ChatMemory chatMemory, AttachmentResolver attachments) {
		super(builder, chatMemory, attachments, SYSTEM_PROMPT);
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
	public List<AgentParameter> parameters() {
		return List.of();
	}

	@Override
	public AgentResponse handle(AgentRequest request) {
		List<StoredDocument> attached = attached(request);
		if (attached.isEmpty()) {
			throw new InvalidAgentRequestException(
					"The document agent needs at least one attached file. Attach a PDF or text file to your message.");
		}

		Completion completion = complete(request, request.message());

		Map<String, Object> metadata = new LinkedHashMap<>(completion.metadata());
		metadata.put("documentIds", attached.stream().map(StoredDocument::id).toList());
		metadata.put("documentNames", attached.stream().map(StoredDocument::name).toList());
		return new AgentResponse(id(), completion.content(), metadata);
	}
}
