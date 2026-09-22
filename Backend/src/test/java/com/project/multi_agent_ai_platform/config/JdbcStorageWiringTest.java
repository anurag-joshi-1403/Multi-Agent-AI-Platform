package com.project.multi_agent_ai_platform.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.project.multi_agent_ai_platform.document.DocumentStore;
import com.project.multi_agent_ai_platform.document.JdbcDocumentStore;

/**
 * The default wiring: both halves of the platform's state go to the database, and both tables
 * exist by the time the context is up.
 * <p>
 * Runs on H2 (see {@code src/test/resources/application.properties}); on a real deployment the same
 * beans sit over the Postgres from {@code compose.yaml}.
 */
@SpringBootTest
class JdbcStorageWiringTest {

	@Autowired
	DocumentStore documentStore;

	@Autowired
	ChatMemoryRepository chatMemoryRepository;

	@Autowired
	JdbcClient jdbc;

	@Test
	void usesTheDatabaseForDocumentsAndConversationMemory() {
		assertThat(documentStore).isInstanceOf(JdbcDocumentStore.class);
		assertThat(chatMemoryRepository).isInstanceOf(JdbcChatMemoryRepository.class);
	}

	@Test
	void createsBothSchemasOnStartup() {
		// platform_documents comes from our schema.sql, SPRING_AI_CHAT_MEMORY from Spring AI's own
		// per-dialect DDL. Two separate initialisers, so a passing boot does not prove both ran.
		assertThat(countOf("platform_documents")).isNotNull();
		assertThat(countOf("SPRING_AI_CHAT_MEMORY")).isNotNull();
	}

	private Integer countOf(String table) {
		return jdbc.sql("SELECT COUNT(*) FROM " + table).query(Integer.class).single();
	}
}
