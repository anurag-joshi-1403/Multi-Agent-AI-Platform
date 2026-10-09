package com.project.multi_agent_ai_platform;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.project.multi_agent_ai_platform.agent.core.Agent;
import com.project.multi_agent_ai_platform.agent.core.AgentRegistry;
import com.project.multi_agent_ai_platform.config.LlmProvider;
import com.project.multi_agent_ai_platform.config.PlatformProperties;
import com.project.multi_agent_ai_platform.document.DocumentStore;
import com.project.multi_agent_ai_platform.document.JdbcDocumentStore;

/** Boots the whole application, on an in-memory H2 database (no datasource is set for tests). */
@SpringBootTest
class MultiAgentAiPlatformApplicationTests {

	@Autowired
	AgentRegistry registry;

	@Autowired
	LlmProvider provider;

	@Autowired
	PlatformProperties properties;

	@Autowired
	DocumentStore documentStore;

	@Autowired
	ChatMemoryRepository chatMemoryRepository;

	@Autowired
	JdbcClient jdbc;

	@Test
	void contextLoadsAndDiscoversTheAgents() {
		assertThat(registry.all()).extracting(Agent::id)
			.containsExactlyInAnyOrder("coding", "general", "research", "summarizer");
	}

	@Test
	void defaultLimits() {
		assertThat(properties.memory().maxMessages()).isEqualTo(20);
		assertThat(properties.documents().maxContextChars()).isEqualTo(60_000);
		assertThat(properties.documents().maxStored()).isEqualTo(50);
	}

	@Test
	void documentsAndConversationMemoryBothLiveInTheDatabase() {
		assertThat(documentStore).isInstanceOf(JdbcDocumentStore.class);
		assertThat(chatMemoryRepository).isInstanceOf(JdbcChatMemoryRepository.class);
	}

	@Test
	void bothTablesAreCreatedOnStartup() {
		// platform_documents comes from our schema.sql, SPRING_AI_CHAT_MEMORY from Spring AI's own DDL:
		// two separate initialisers, so a passing boot alone does not prove both ran
		assertThat(countOf("platform_documents")).isNotNull();
		assertThat(countOf("SPRING_AI_CHAT_MEMORY")).isNotNull();
	}

	@Test
	void testsUseTheTestConfigNeverBackendEnv() {
		// gemini-test only exists in src/test/resources/application.properties
		assertThat(provider.model()).isEqualTo("gemini-test");
	}

	private Integer countOf(String table) {
		return jdbc.sql("SELECT COUNT(*) FROM " + table).query(Integer.class).single();
	}
}
