package com.project.multi_agent_ai_platform.config;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import com.project.multi_agent_ai_platform.document.DocumentStore;
import com.project.multi_agent_ai_platform.document.InMemoryDocumentStore;

/**
 * The {@code memory} profile has one job: let the platform run with no database at all. That is
 * only true if the context comes up with no {@link DataSource} in it — the Postgres driver is on
 * the classpath, and Spring AI's JDBC chat-memory auto-configuration registers a schema initializer
 * that wants a datasource, so both have to be excluded rather than merely unused.
 * <p>
 * Asserting the absence is the point: this test failing means someone running
 * {@code -Dspring-boot.run.profiles=memory} without Docker gets a startup failure instead.
 */
@SpringBootTest
@ActiveProfiles("memory")
class MemoryStorageWiringTest {

	@Autowired
	ApplicationContext context;

	@Autowired
	DocumentStore documentStore;

	@Autowired
	ChatMemoryRepository chatMemoryRepository;

	@Test
	void keepsDocumentsAndConversationMemoryInRam() {
		assertThat(documentStore).isInstanceOf(InMemoryDocumentStore.class);
		assertThat(chatMemoryRepository).isInstanceOf(InMemoryChatMemoryRepository.class);
	}

	@Test
	void needsNoDatabase() {
		assertThat(context.getBeanNamesForType(DataSource.class)).isEmpty();
	}
}
