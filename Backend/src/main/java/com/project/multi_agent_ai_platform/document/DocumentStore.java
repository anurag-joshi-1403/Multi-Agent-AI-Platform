package com.project.multi_agent_ai_platform.document;

import java.util.Optional;

/**
 * Where uploaded documents live between the upload call and the agent calls that read them.
 * <p>
 * {@link JdbcDocumentStore} is the implementation; nothing above this interface - not
 * {@code DocumentController}, not {@link AttachmentResolver}, not any agent - depends on it, so
 * tests can swap in a fake.
 * <p>
 * Capacity is bounded by {@code platform.documents.max-stored}: saving past it deletes the oldest
 * upload. {@link AttachmentResolver} expects that, so a conversation that still lists a deleted id
 * keeps working without that file.
 */
public interface DocumentStore {

	/**
	 * Store extracted text under a freshly generated id.
	 *
	 * @param pages page count for PDFs, {@code null} for anything else
	 */
	StoredDocument save(String name, String mediaType, String content, Integer pages);

	/** The document, or {@link Optional#empty()} when the id is unknown, {@code null} or deleted. */
	Optional<StoredDocument> find(String id);

	/** @return {@code true} if the id existed; {@code false} makes repeat deletes harmless */
	boolean delete(String id);

	int size();
}
