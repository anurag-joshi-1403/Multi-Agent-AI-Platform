package com.project.multi_agent_ai_platform.document;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.project.multi_agent_ai_platform.config.PlatformProperties;

/**
 * The original {@link DocumentStore}: a bounded map, wiped on restart. Selected by
 * {@code platform.storage=memory}, which is what the {@code memory} profile sets — it is the only
 * way to run this platform with no database at all, and it is what the web-layer slice tests use
 * so they stay free of a datasource.
 */
public class InMemoryDocumentStore implements DocumentStore {

	private final Map<String, StoredDocument> documents = new ConcurrentHashMap<>();

	private final int maxStored;

	public InMemoryDocumentStore(PlatformProperties properties) {
		this.maxStored = Math.max(1, properties.documents().maxStored());
	}

	@Override
	public StoredDocument save(String name, String mediaType, String content, Integer pages) {
		StoredDocument doc = new StoredDocument(DocumentIds.newId(), name, mediaType, content, pages,
				DocumentStore.now());
		documents.put(doc.id(), doc);
		evictIfNeeded();
		return doc;
	}

	@Override
	public Optional<StoredDocument> find(String id) {
		return Optional.ofNullable(id).map(documents::get);
	}

	@Override
	public boolean delete(String id) {
		// ConcurrentHashMap#remove throws on a null key, where "delete nothing" is the honest answer
		// and is what the JDBC store does.
		return id != null && documents.remove(id) != null;
	}

	@Override
	public List<StoredDocument> all() {
		List<StoredDocument> list = new ArrayList<>(documents.values());
		list.sort(Comparator.comparing(StoredDocument::uploadedAt).reversed());
		return list;
	}

	@Override
	public int size() {
		return documents.size();
	}

	private void evictIfNeeded() {
		while (documents.size() > maxStored) {
			documents.values().stream()
				.min(Comparator.comparing(StoredDocument::uploadedAt))
				.ifPresent(oldest -> documents.remove(oldest.id()));
		}
	}
}
