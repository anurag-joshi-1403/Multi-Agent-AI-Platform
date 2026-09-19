package com.project.multi_agent_ai_platform.document;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.project.multi_agent_ai_platform.config.PlatformProperties;

/**
 * In-memory document store with a fixed capacity: once {@code platform.documents.max-stored} is
 * reached the oldest upload is evicted. Good enough for a single-node developer platform; a
 * persistent store can replace it without touching the agent or the controller.
 */
@Component
public class DocumentStore {

	private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

	private final Map<String, StoredDocument> documents = new ConcurrentHashMap<>();

	private final SecureRandom random = new SecureRandom();

	private final int maxStored;

	public DocumentStore(PlatformProperties properties) {
		this.maxStored = Math.max(1, properties.documents().maxStored());
	}

	public StoredDocument save(String name, String mediaType, String content, Integer pages) {
		String id = newId();
		StoredDocument doc = new StoredDocument(id, name, mediaType, content, pages, Instant.now());
		documents.put(id, doc);
		evictIfNeeded();
		return doc;
	}

	public StoredDocument get(String id) {
		return find(id).orElseThrow(() -> new DocumentNotFoundException(id));
	}

	public Optional<StoredDocument> find(String id) {
		return Optional.ofNullable(id).map(documents::get);
	}

	public boolean delete(String id) {
		return documents.remove(id) != null;
	}

	/** Newest first. */
	public List<StoredDocument> all() {
		List<StoredDocument> list = new ArrayList<>(documents.values());
		list.sort(Comparator.comparing(StoredDocument::uploadedAt).reversed());
		return list;
	}

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

	private String newId() {
		StringBuilder sb = new StringBuilder("doc_");
		for (int i = 0; i < 12; i++) {
			sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
		}
		return sb.toString();
	}
}
