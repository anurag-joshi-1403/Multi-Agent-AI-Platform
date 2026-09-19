package com.project.multi_agent_ai_platform.document;

import java.time.Instant;

/**
 * A document whose text has been extracted and is ready for the document agent.
 *
 * @param id         server-generated, URL-safe
 * @param name       original file name (or the name given for pasted text)
 * @param mediaType  what was uploaded, e.g. {@code application/pdf}
 * @param content    extracted plain text; PDF pages are separated by {@code [page N]} markers
 * @param pages      page count for PDFs, {@code null} otherwise
 * @param uploadedAt when it was stored
 */
public record StoredDocument(String id, String name, String mediaType, String content, Integer pages, Instant uploadedAt) {
}
