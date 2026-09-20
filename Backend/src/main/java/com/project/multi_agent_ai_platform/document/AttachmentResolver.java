package com.project.multi_agent_ai_platform.document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.project.multi_agent_ai_platform.agent.core.AgentRequest;
import com.project.multi_agent_ai_platform.config.PlatformProperties;

/**
 * Turns the {@code attachments} request attribute into model context. Every LLM agent resolves
 * attachments through this, so a file can be attached in any conversation rather than only the
 * document agent's.
 */
@Component
public class AttachmentResolver {

	/** Attribute key holding the ids of the documents attached to a conversation. */
	public static final String ATTRIBUTE = "attachments";

	private final DocumentStore documents;

	private final int maxContextChars;

	public AttachmentResolver(DocumentStore documents, PlatformProperties properties) {
		this.documents = documents;
		this.maxContextChars = properties.documents().maxContextChars();
	}

	/**
	 * Attached documents in request order, without duplicates. Ids the store has already evicted
	 * are skipped rather than failing the call: the conversation replays every id it has ever
	 * attached, so an old one dropping out must not break the current question.
	 */
	public List<StoredDocument> resolve(AgentRequest request) {
		List<StoredDocument> resolved = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>();
		for (String id : ids(request)) {
			if (seen.add(id)) {
				documents.find(id).ifPresent(resolved::add);
			}
		}
		return resolved;
	}

	/**
	 * System-prompt section describing {@code attached}, or {@code null} when there is nothing to
	 * add. The text rides in the per-call system prompt so it never enters conversation memory.
	 * The character budget is shared evenly so one huge file cannot crowd out the rest.
	 */
	public String block(List<StoredDocument> attached) {
		if (attached.isEmpty()) {
			return null;
		}
		int budget = Math.max(1, maxContextChars / attached.size());
		StringBuilder sb = new StringBuilder("\n\nThe user attached the following file(s). Use them to answer.\n");
		for (StoredDocument doc : attached) {
			String text = doc.content();
			boolean truncated = text.length() > budget;
			if (truncated) {
				text = text.substring(0, budget);
			}
			sb.append("\n--- FILE: ").append(doc.name()).append(truncated ? " (truncated)" : "").append(" ---\n")
				.append(text).append("\n--- END OF FILE ---\n");
		}
		return sb.toString();
	}

	/** Accepts a list of ids or a single id, so a one-file caller need not wrap it in an array. */
	private static List<String> ids(AgentRequest request) {
		Object value = request.attributes().get(ATTRIBUTE);
		if (value instanceof Collection<?> collection) {
			return collection.stream()
				.filter(Objects::nonNull)
				.map(String::valueOf)
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toList();
		}
		if (value != null && !String.valueOf(value).isBlank()) {
			return List.of(String.valueOf(value).trim());
		}
		return List.of();
	}
}
