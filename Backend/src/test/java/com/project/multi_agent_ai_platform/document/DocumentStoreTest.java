package com.project.multi_agent_ai_platform.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.multi_agent_ai_platform.config.PlatformProperties;

class DocumentStoreTest {

	private static DocumentStore storeWithCapacity(int maxStored) {
		return new DocumentStore(new PlatformProperties(new PlatformProperties.Cors(List.of()),
				new PlatformProperties.Memory(20), new PlatformProperties.Documents(1000, maxStored)));
	}

	@Test
	void savesAndFindsById() {
		DocumentStore store = storeWithCapacity(10);

		StoredDocument saved = store.save("a.txt", "text/plain", "alpha", null);

		assertThat(saved.id()).startsWith("doc_").hasSize(16);
		assertThat(store.get(saved.id()).content()).isEqualTo("alpha");
		assertThat(store.find("doc_missing")).isEmpty();
		assertThatThrownBy(() -> store.get("doc_missing")).isInstanceOf(DocumentNotFoundException.class);
	}

	@Test
	void evictsOldestBeyondCapacity() throws InterruptedException {
		DocumentStore store = storeWithCapacity(2);

		StoredDocument first = store.save("1", "text/plain", "1", null);
		Thread.sleep(2);
		store.save("2", "text/plain", "2", null);
		Thread.sleep(2);
		StoredDocument third = store.save("3", "text/plain", "3", null);

		assertThat(store.size()).isEqualTo(2);
		assertThat(store.find(first.id())).isEmpty();
		assertThat(store.find(third.id())).isPresent();
		assertThat(store.all()).extracting(StoredDocument::name).containsExactly("3", "2");
	}

	@Test
	void deleteIsIdempotent() {
		DocumentStore store = storeWithCapacity(10);
		StoredDocument doc = store.save("a", "text/plain", "a", null);

		assertThat(store.delete(doc.id())).isTrue();
		assertThat(store.delete(doc.id())).isFalse();
	}
}
