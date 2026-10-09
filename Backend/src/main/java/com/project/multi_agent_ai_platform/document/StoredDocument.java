package com.project.multi_agent_ai_platform.document;

import java.time.Instant;

/**
 * An uploaded file whose text has been extracted and is ready for the agents.
 *
 * @param id         server-generated, URL-safe, e.g. {@code doc_k3x9...}
 * @param name       original file name
 * @param mediaType  what was uploaded, e.g. {@code application/pdf}
 * @param content    extracted plain text; PDF pages are separated by {@code [page N]} markers
 * @param pages      page count for PDFs, {@code null} otherwise
 * @param uploadedAt when it was stored
 */
public record StoredDocument(String id, String name, String mediaType, String content, Integer pages,
		Instant uploadedAt) {
}
