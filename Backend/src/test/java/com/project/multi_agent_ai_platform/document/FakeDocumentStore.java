package com.project.multi_agent_ai_platform.document;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** A {@link DocumentStore} in a map, for tests that should not need a database. */
public class FakeDocumentStore implements DocumentStore {

	private final Map<String, StoredDocument> documents = new LinkedHashMap<>();

	private int nextId = 1;

	@Override
	public StoredDocument save(String name, String mediaType, String content, Integer pages) {
		StoredDocument doc = new StoredDocument("doc_test" + nextId++, name, mediaType, content, pages, Instant.now());
		documents.put(doc.id(), doc);
		return doc;
	}

	@Override
	public Optional<StoredDocument> find(String id) {
		return Optional.ofNullable(id).map(documents::get);
	}

	@Override
	public boolean delete(String id) {
		return id != null && documents.remove(id) != null;
	}

	@Override
	public int size() {
		return documents.size();
	}
}
