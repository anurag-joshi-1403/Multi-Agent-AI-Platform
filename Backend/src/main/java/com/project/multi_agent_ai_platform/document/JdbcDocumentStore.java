package com.project.multi_agent_ai_platform.document;

import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.project.multi_agent_ai_platform.config.PlatformProperties;

/**
 * Uploads in the {@code platform_documents} table (see {@code schema.sql}), so they survive a
 * restart. Plain {@link JdbcClient} rather than JPA: a document is one flat row, and the extracted
 * text is already a {@code String} by the time it gets here.
 */
@Component
public class JdbcDocumentStore implements DocumentStore {

	private static final String COLUMNS = "id, name, media_type, content, pages, uploaded_at";

	/** Shared by lookups and eviction so "oldest" means the same thing everywhere. */
	private static final String NEWEST_FIRST = " ORDER BY uploaded_at DESC, id DESC";

	private static final String ID_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

	private static final SecureRandom RANDOM = new SecureRandom();

	private final JdbcClient jdbc;

	private final int maxStored;

	public JdbcDocumentStore(JdbcClient jdbc, PlatformProperties properties) {
		this.jdbc = jdbc;
		this.maxStored = Math.max(1, properties.documents().maxStored());
	}

	@Override
	@Transactional
	public StoredDocument save(String name, String mediaType, String content, Integer pages) {
		StoredDocument doc = new StoredDocument(newId(), name, mediaType, content, pages, now());
		jdbc.sql("INSERT INTO platform_documents (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?)")
			.params(doc.id(), doc.name(), doc.mediaType(), doc.content(), doc.pages(),
					OffsetDateTime.ofInstant(doc.uploadedAt(), ZoneOffset.UTC))
			.update();
		evictIfNeeded();
		return doc;
	}

	@Override
	public Optional<StoredDocument> find(String id) {
		if (id == null) {
			return Optional.empty();
		}
		return jdbc.sql("SELECT " + COLUMNS + " FROM platform_documents WHERE id = ?")
			.param(id)
			.query(JdbcDocumentStore::toDocument)
			.optional();
	}

	@Override
	public boolean delete(String id) {
		if (id == null) {
			return false;
		}
		return jdbc.sql("DELETE FROM platform_documents WHERE id = ?").param(id).update() > 0;
	}

	@Override
	public int size() {
		return jdbc.sql("SELECT COUNT(*) FROM platform_documents").query(Integer.class).single();
	}

	/**
	 * Keep only the newest {@code max-stored} rows, in one statement rather than select-then-delete
	 * so two concurrent uploads cannot both pick the same oldest row.
	 */
	private void evictIfNeeded() {
		jdbc.sql("DELETE FROM platform_documents WHERE id NOT IN "
				+ "(SELECT id FROM platform_documents" + NEWEST_FIRST + " LIMIT ?)")
			.param(maxStored)
			.update();
	}

	/** {@code doc_} plus 12 random lowercase alphanumerics: URL-safe and not guessable in sequence. */
	private static String newId() {
		StringBuilder sb = new StringBuilder("doc_");
		for (int i = 0; i < 12; i++) {
			sb.append(ID_ALPHABET.charAt(RANDOM.nextInt(ID_ALPHABET.length())));
		}
		return sb.toString();
	}

	/**
	 * {@link Instant#now()} has nanoseconds, but {@code TIMESTAMP} keeps microseconds. Truncating up
	 * front means {@link #save} returns the same timestamp a later {@link #find} reads back.
	 */
	private static Instant now() {
		return Instant.now().truncatedTo(ChronoUnit.MICROS);
	}

	private static StoredDocument toDocument(ResultSet rs, int rowNum) throws SQLException {
		// getObject(..., Integer.class) rather than getInt: a null `pages` must stay null, not 0.
		// OffsetDateTime rather than Timestamp: the value does not depend on the server's time zone.
		Integer pages = rs.getObject("pages", Integer.class);
		OffsetDateTime uploadedAt = rs.getObject("uploaded_at", OffsetDateTime.class);
		return new StoredDocument(rs.getString("id"), rs.getString("name"), rs.getString("media_type"),
				rs.getString("content"), pages, uploadedAt.toInstant());
	}
}
