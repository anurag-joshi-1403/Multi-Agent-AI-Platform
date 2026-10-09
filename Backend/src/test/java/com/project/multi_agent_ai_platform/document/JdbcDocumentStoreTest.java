package com.project.multi_agent_ai_platform.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import com.project.multi_agent_ai_platform.config.PlatformProperties;

/** Runs on in-memory H2 against the real {@code schema.sql}, so the shipped DDL is the DDL under test. */
class JdbcDocumentStoreTest {

	private final List<EmbeddedDatabase> databases = new ArrayList<>();

	@AfterEach
	void closeDatabases() {
		databases.forEach(EmbeddedDatabase::shutdown);
	}

	/** A database of its own per call, so no test sees another's rows. */
	private JdbcClient newDatabase() {
		EmbeddedDatabase database = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
			.generateUniqueName(true)
			.addScript("classpath:schema.sql")
			.build();
		databases.add(database);
		return JdbcClient.create(database);
	}

	private static PlatformProperties maxStored(int maxStored) {
		return new PlatformProperties(new PlatformProperties.Memory(20), new PlatformProperties.Documents(1000, maxStored),
				new PlatformProperties.Auth(""));
	}

	private JdbcDocumentStore store(int maxStored) {
		return new JdbcDocumentStore(newDatabase(), maxStored(maxStored));
	}

	@Test
	void savesAndFindsById() {
		JdbcDocumentStore store = store(10);

		StoredDocument saved = store.save("a.txt", "text/plain", "alpha", null);

		assertThat(saved.id()).startsWith("doc_").hasSize(16).matches("doc_[a-z0-9]{12}");
		assertThat(store.find(saved.id())).get().extracting(StoredDocument::content).isEqualTo("alpha");
		assertThat(store.find("doc_missing")).isEmpty();
		assertThat(store.find(null)).isEmpty();
		assertThat(store.size()).isEqualTo(1);
	}

	@Test
	void roundTripsEveryField() {
		JdbcDocumentStore store = store(10);

		StoredDocument saved = store.save("report.pdf", "application/pdf", "[page 1] hello", 7);

		StoredDocument found = store.find(saved.id()).orElseThrow();
		assertThat(found).isEqualTo(saved);
		// a null `pages` must come back null, not 0
		StoredDocument noPages = store.save("notes.txt", "text/plain", "plain", null);
		assertThat(store.find(noPages.id()).orElseThrow().pages()).isNull();
	}

	@Test
	void evictsTheOldestBeyondCapacity() throws InterruptedException {
		JdbcDocumentStore store = store(2);

		StoredDocument first = store.save("1", "text/plain", "1", null);
		Thread.sleep(2);
		StoredDocument second = store.save("2", "text/plain", "2", null);
		Thread.sleep(2);
		StoredDocument third = store.save("3", "text/plain", "3", null);

		assertThat(store.size()).isEqualTo(2);
		assertThat(store.find(first.id())).isEmpty();
		assertThat(store.find(second.id())).isPresent();
		assertThat(store.find(third.id())).isPresent();
	}

	@Test
	void deleteIsIdempotent() {
		JdbcDocumentStore store = store(10);
		StoredDocument doc = store.save("a", "text/plain", "a", null);

		assertThat(store.delete(doc.id())).isTrue();
		assertThat(store.delete(doc.id())).isFalse();
		assertThat(store.delete(null)).isFalse();
		assertThat(store.size()).isZero();
	}

	/** What "survives a restart" reduces to: a new store over the same database sees the old rows. */
	@Test
	void outlivesTheInstanceThatWroteIt() {
		JdbcClient jdbc = newDatabase();

		StoredDocument saved = new JdbcDocumentStore(jdbc, maxStored(10))
			.save("contract.pdf", "application/pdf", "renews automatically", 3);

		JdbcDocumentStore reopened = new JdbcDocumentStore(jdbc, maxStored(10));
		assertThat(reopened.find(saved.id())).contains(saved);
	}
}
