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
import com.project.multi_agent_ai_platform.config.PlatformProperties;
import com.project.multi_agent_ai_platform.document.DocumentStore;
import com.project.multi_agent_ai_platform.document.StoredDocument;

/**
 * Answers questions grounded in an uploaded document. The caller uploads via
 * {@code POST /api/documents} and passes the returned id as the {@code documentId} attribute.
 * The document text travels in the per-call system prompt, so conversation memory stays small.
 */
@Component
public class DocumentAgent extends LlmAgent {

	static final String SYSTEM_PROMPT = """
			You answer questions strictly from the document provided below.
			- Quote the relevant passage briefly (with its page marker when present) before answering.
			- If the document does not contain the answer, say exactly that; do not guess from outside knowledge.
			- Be concise.
			""";

	private final DocumentStore documents;

	private final int maxContextChars;

	public DocumentAgent(ChatClient.Builder builder, ChatMemory chatMemory, DocumentStore documents,
			PlatformProperties properties) {
		super(builder, chatMemory, SYSTEM_PROMPT);
		this.documents = documents;
		this.maxContextChars = properties.documents().maxContextChars();
	}

	@Override
	public String id() {
		return "document";
	}

	@Override
	public String description() {
		return "Answers questions grounded in an uploaded PDF or text document (pass documentId in attributes).";
	}

	@Override
	public List<String> capabilities() {
		return List.of("PDF Q&A", "Extraction", "Grounded answers");
	}

	@Override
	public List<AgentParameter> parameters() {
		return List.of(AgentParameter.document("documentId", "Document",
				"An uploaded PDF or text file to answer from."));
	}

	@Override
	public AgentResponse handle(AgentRequest request) {
		String documentId = attribute(request, "documentId");
		if (documentId == null) {
			throw new InvalidAgentRequestException(
					"The document agent needs a documentId attribute. Upload a file to POST /api/documents first.");
		}
		StoredDocument document = documents.get(documentId);

		String text = document.content();
		boolean truncated = text.length() > maxContextChars;
		if (truncated) {
			text = text.substring(0, maxContextChars);
		}
		String system = SYSTEM_PROMPT + "\n--- DOCUMENT: " + document.name() + (truncated ? " (truncated)" : "")
				+ " ---\n" + text + "\n--- END OF DOCUMENT ---";

		Completion completion = complete(request, request.message(), system);

		Map<String, Object> metadata = new LinkedHashMap<>(completion.metadata());
		metadata.put("documentId", document.id());
		metadata.put("documentName", document.name());
		if (document.pages() != null) {
			metadata.put("pages", document.pages());
		}
		metadata.put("contextChars", text.length());
		metadata.put("truncated", truncated);
		return new AgentResponse(id(), completion.content(), metadata);
	}
}
