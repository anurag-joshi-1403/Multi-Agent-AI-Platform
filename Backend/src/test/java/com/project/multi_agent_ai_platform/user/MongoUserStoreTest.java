package com.project.multi_agent_ai_platform.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;

/**
 * Against a real MongoDB ({@code agents_test} database from the test properties), so MongoDB must be
 * running. The collection is emptied before each test, and the indexes declared on
 * {@link UserDocument} are created the same way {@code auto-index-creation} does at startup.
 */
@SpringBootTest
class MongoUserStoreTest {

	@Autowired
	MongoUserStore store;

	@Autowired
	UserRepository repository;

	@Autowired
	MongoTemplate mongo;

	@BeforeEach
	void emptyCollectionWithItsIndexes() {
		mongo.dropCollection(UserDocument.class);
		IndexOperations indexes = mongo.indexOps(UserDocument.class);
		IndexResolver.create(mongo.getConverter().getMappingContext())
			.resolveIndexFor(UserDocument.class)
			.forEach(indexes::createIndex);
	}

	@Test
	void savesAnAccountAndFindsItByUsername() {
		UserAccount created = store.create("anna", "$2a$10$hash");

		UserAccount found = store.findByUsername("anna").orElseThrow();
		assertThat(found.id()).isEqualTo(created.id()).isNotBlank();
		assertThat(found.passwordHash()).isEqualTo("$2a$10$hash");
		assertThat(found.role()).isEqualTo("USER");
		assertThat(found.createdAt()).isNotNull();
		assertThat(store.findByUsername("nobody")).isEmpty();
	}

	@Test
	void aTakenUsernameIsRefused() {
		store.create("anna", "hash-1");

		assertThatThrownBy(() -> store.create("anna", "hash-2"))
			.isInstanceOf(UsernameTakenException.class)
			.hasMessageContaining("anna");
		assertThat(repository.count()).isEqualTo(1);
	}

	@Test
	void theDatabaseItselfRefusesADuplicateUsername() {
		// What stops two sign-ups that arrive at the same moment: the unique index, not the check in code
		assertThat(mongo.indexOps(UserDocument.class).getIndexInfo())
			.filteredOn(IndexInfo::isUnique)
			.extracting(IndexInfo::getName)
			.contains("username");

		UserDocument first = new UserDocument();
		first.setUsername("anna");
		repository.save(first);
		UserDocument second = new UserDocument();
		second.setUsername("anna");

		assertThatThrownBy(() -> repository.save(second)).isInstanceOf(DuplicateKeyException.class);
	}
}
