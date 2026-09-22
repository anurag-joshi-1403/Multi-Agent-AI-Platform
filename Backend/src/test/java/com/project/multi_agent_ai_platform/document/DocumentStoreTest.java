package com.project.multi_agent_ai_platform.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import com.project.multi_agent_ai_platform.config.PlatformProperties;

/**
 * One set of assertions, run against every {@link DocumentStore} implementation.
 * <p>
 * The point is the interface, not either class: a document saved through the contract must come
 * back the same way, eviction must pick the same victim, and a repeat delete must be harmless,
 * whether the rows live in a {@code ConcurrentHashMap} or in a database. A difference that only
 * shows up for one implementation is exactly what would otherwise surface after switching
 * {@code platform.storage} in production.
 * <p>
 * The JDBC side runs on in-memory H2 against the real {@code schema.sql}, so no database server is
 * involved and the shipped DDL is the DDL under test.
 */
class DocumentStoreTest {

	private enum Impl {

		IN_MEMORY, JDBC

	}

	/** Embedded databases opened during a test, shut down when it ends. */
	private final List<EmbeddedDatabase> databases = new ArrayList<>();

	@AfterEach
	void closeDatabases() {
		databases.forEach(EmbeddedDatabase::shutdown);
		databases.clear();
	}

	private DocumentStore store(Impl impl, int maxStored) {
		return switch (impl) {
			case IN_MEMORY -> new InMemoryDocumentStore(properties(maxStored));
			case JDBC -> new JdbcDocumentStore(JdbcClient.create(newDatabase()), properties(maxStored));
		};
	}

	/** A database of its own per call, so no test can see another's rows. */
	private EmbeddedDatabase newDatabase() {
		EmbeddedDatabase database = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
			.generateUniqueName(true)
			.addScript("classpath:schema.sql")
			.build();
		databases.add(database);
		return database;
	}

	private static PlatformProperties properties(int maxStored) {
		return new PlatformProperties(PlatformProperties.Storage.JDBC, new PlatformProperties.Cors(List.of()),
				new PlatformProperties.Memory(20), new PlatformProperties.Documents(1000, maxStored),
				new PlatformProperties.Auth("tester:tester"));
	}

	@ParameterizedTest
	@EnumSource(Impl.class)
	void savesAndFindsById(Impl impl) {
		DocumentStore store = store(impl, 10);

		StoredDocument saved = store.save("a.txt", "text/plain", "alpha", null);

		assertThat(saved.id()).startsWith("doc_").hasSize(16);
		assertThat(store.get(saved.id()).content()).isEqualTo("alpha");
		assertThat(store.find("doc_missing")).isEmpty();
		assertThat(store.find(null)).isEmpty();
		assertThatThrownBy(() -> store.get("doc_missing")).isInstanceOf(DocumentNotFoundException.class);
	}

	@ParameterizedTest
	@EnumSource(Impl.class)
	void roundTripsEveryField(Impl impl) {
		DocumentStore store = store(impl, 10);

		StoredDocument saved = store.save("report.pdf", "application/pdf", "[page 1] hello", 7);

		// Read back rather than trusting what save() returned: the JDBC store has to survive a trip
		// through the database, where a null `pages` could come back as 0 if it were read as an int.
		StoredDocument found = store.get(saved.id());
		assertThat(found.name()).isEqualTo("report.pdf");
		assertThat(found.mediaType()).isEqualTo("application/pdf");
		assertThat(found.content()).isEqualTo("[page 1] hello");
		assertThat(found.pages()).isEqualTo(7);
		assertThat(found.uploadedAt()).isEqualTo(saved.uploadedAt());

		StoredDocument noPages = store.save("notes.txt", "text/plain", "plain", null);
		assertThat(store.get(noPages.id()).pages()).isNull();
	}

	@ParameterizedTest
	@EnumSource(Impl.class)
	void evictsOldestBeyondCapacity(Impl impl) throws InterruptedException {
		DocumentStore store = store(impl, 2);

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

	@ParameterizedTest
	@EnumSource(Impl.class)
	void deleteIsIdempotent(Impl impl) {
		DocumentStore store = store(impl, 10);
		StoredDocument doc = store.save("a", "text/plain", "a", null);

		assertThat(store.delete(doc.id())).isTrue();
		assertThat(store.delete(doc.id())).isFalse();
		assertThat(store.delete(null)).isFalse();
		assertThat(store.size()).isZero();
	}

	/**
	 * The whole point of the JDBC store: a second instance over the same database sees what the
	 * first one wrote. That is what "survives a restart" reduces to, minus the JVM restart — and it
	 * is the one behaviour the in-memory store cannot have, so it is asserted only here.
	 */
	@Test
	void jdbcStoreOutlivesTheInstanceThatWroteIt() {
		JdbcClient jdbc = JdbcClient.create(newDatabase());

		StoredDocument saved = new JdbcDocumentStore(jdbc, properties(10))
			.save("contract.pdf", "application/pdf", "renews automatically", 3);

		DocumentStore reopened = new JdbcDocumentStore(jdbc, properties(10));
		assertThat(reopened.get(saved.id()).content()).isEqualTo("renews automatically");
		assertThat(reopened.all()).extracting(StoredDocument::name).containsExactly("contract.pdf");
	}
}
