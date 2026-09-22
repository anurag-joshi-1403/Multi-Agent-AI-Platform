package com.project.multi_agent_ai_platform.document;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import com.project.multi_agent_ai_platform.config.PlatformProperties;

/**
 * The default {@link DocumentStore}: uploads live in the {@code platform_documents} table and
 * survive a restart. Plain {@link JdbcClient} rather than JPA — a document is one flat row, and
 * the extracted text is already a {@code String} by the time it gets here.
 * <p>
 * Schema in {@code schema.sql}, applied on boot; see {@code application.properties} for how the
 * datasource is wired.
 */
public class JdbcDocumentStore implements DocumentStore {

	private static final String COLUMNS = "id, name, media_type, content, pages, uploaded_at";

	/** Shared by {@link #all()} and eviction so "oldest" and "newest first" cannot disagree. */
	private static final String NEWEST_FIRST = " ORDER BY uploaded_at DESC, id DESC";

	private final JdbcClient jdbc;

	private final int maxStored;

	public JdbcDocumentStore(JdbcClient jdbc, PlatformProperties properties) {
		this.jdbc = jdbc;
		this.maxStored = Math.max(1, properties.documents().maxStored());
	}

	@Override
	@Transactional
	public StoredDocument save(String name, String mediaType, String content, Integer pages) {
		StoredDocument doc = new StoredDocument(DocumentIds.newId(), name, mediaType, content, pages,
				DocumentStore.now());
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
	public List<StoredDocument> all() {
		return jdbc.sql("SELECT " + COLUMNS + " FROM platform_documents" + NEWEST_FIRST)
			.query(JdbcDocumentStore::toDocument)
			.list();
	}

	@Override
	public int size() {
		return jdbc.sql("SELECT COUNT(*) FROM platform_documents").query(Integer.class).single();
	}

	/**
	 * Keep only the newest {@code max-stored} rows. Expressed as one statement rather than
	 * select-then-delete so two concurrent uploads cannot each decide the same row is the oldest
	 * and race; the {@code LIMIT} inside a {@code NOT IN} subquery is valid on both Postgres and H2.
	 */
	private void evictIfNeeded() {
		jdbc.sql("DELETE FROM platform_documents WHERE id NOT IN "
				+ "(SELECT id FROM platform_documents" + NEWEST_FIRST + " LIMIT ?)")
			.param(maxStored)
			.update();
	}

	private static StoredDocument toDocument(ResultSet rs, int rowNum) throws SQLException {
		// getObject(..., Integer.class) rather than getInt: a null `pages` must stay null, not 0.
		// OffsetDateTime rather than java.sql.Timestamp: the column carries its own offset, so the
		// value read back does not depend on the server's default time zone at the time of reading.
		Integer pages = rs.getObject("pages", Integer.class);
		OffsetDateTime uploadedAt = rs.getObject("uploaded_at", OffsetDateTime.class);
		return new StoredDocument(rs.getString("id"), rs.getString("name"), rs.getString("media_type"),
				rs.getString("content"), pages, uploadedAt.toInstant());
	}
}
