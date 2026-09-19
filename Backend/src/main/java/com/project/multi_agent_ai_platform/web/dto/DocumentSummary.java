package com.project.multi_agent_ai_platform.web.dto;

import java.time.Instant;

import com.project.multi_agent_ai_platform.document.StoredDocument;

/** Document metadata without the (potentially huge) text. */
public record DocumentSummary(String id, String name, String mediaType, int chars, Integer pages, Instant uploadedAt,
		String preview) {

	private static final int PREVIEW_CHARS = 200;

	public static DocumentSummary of(StoredDocument doc) {
		String content = doc.content();
		String preview = content.length() <= PREVIEW_CHARS ? content : content.substring(0, PREVIEW_CHARS) + "...";
		return new DocumentSummary(doc.id(), doc.name(), doc.mediaType(), content.length(), doc.pages(),
				doc.uploadedAt(), preview.replaceAll("\\s+", " ").strip());
	}
}
