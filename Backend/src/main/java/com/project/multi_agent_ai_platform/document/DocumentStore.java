package com.project.multi_agent_ai_platform.document;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Where uploaded documents live between the upload call and the agent call that reads them.
 * <p>
 * Two implementations ship: {@link JdbcDocumentStore}, the default, which survives a restart, and
 * {@link InMemoryDocumentStore}, which does not but needs no database. {@code platform.storage}
 * picks one. Nothing above this interface — not {@code DocumentController}, not
 * {@link AttachmentResolver}, not any agent — knows which is in use.
 * <p>
 * Both honour {@code platform.documents.max-stored}: once that many documents are held, saving a
 * new one evicts the oldest. {@link AttachmentResolver} is built to expect that, so a conversation
 * replaying an id that has since been evicted degrades instead of failing.
 */
public interface DocumentStore {

	/**
	 * Store extracted text under a freshly generated id.
	 *
	 * @param pages page count for PDFs, {@code null} for anything else
	 */
	StoredDocument save(String name, String mediaType, String content, Integer pages);

	/** The document, or {@link Optional#empty()} when the id is unknown, {@code null} or evicted. */
	Optional<StoredDocument> find(String id);

	/** @return {@code true} if the id existed; {@code false} makes repeat deletes harmless */
	boolean delete(String id);

	/** Every stored document, newest first. */
	List<StoredDocument> all();

	int size();

	/** Like {@link #find(String)}, for callers that treat a missing document as a {@code 404}. */
	default StoredDocument get(String id) {
		return find(id).orElseThrow(() -> new DocumentNotFoundException(id));
	}

	/**
	 * The upload timestamp every implementation stamps a document with, truncated to microseconds.
	 * <p>
	 * {@link Instant#now()} is nanosecond-resolution on modern JDKs, but {@code TIMESTAMP} keeps only
	 * microseconds in both PostgreSQL and H2. Without truncating here, {@link #save} would hand back
	 * a document whose {@code uploadedAt} no later {@link #find} could reproduce — the two would
	 * differ in the last three digits. Rounding once, up front, keeps the stores agreeing.
	 */
	static Instant now() {
		return Instant.now().truncatedTo(ChronoUnit.MICROS);
	}
}
