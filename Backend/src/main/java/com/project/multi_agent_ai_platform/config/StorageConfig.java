package com.project.multi_agent_ai_platform.config;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.project.multi_agent_ai_platform.document.DocumentStore;
import com.project.multi_agent_ai_platform.document.InMemoryDocumentStore;
import com.project.multi_agent_ai_platform.document.JdbcDocumentStore;

/**
 * Picks the persistence backend from {@code platform.storage}. Both halves of the platform's state
 * move together, so there is one switch rather than one per store:
 * <table>
 * <tr><th></th><th>{@code jdbc} (default)</th><th>{@code memory}</th></tr>
 * <tr><td>Documents</td><td>{@link JdbcDocumentStore}</td><td>{@link InMemoryDocumentStore}</td></tr>
 * <tr><td>Conversation memory</td><td>Spring AI's JDBC repository</td><td>Spring AI's in-memory one</td></tr>
 * </table>
 * <p>
 * Only the {@code memory} side is declared here. The {@code jdbc} chat-memory repository comes from
 * Spring AI's own auto-configuration, which backs off from any {@link ChatMemoryRepository} already
 * defined — so declaring the in-memory one is what switches it off.
 */
@Configuration
public class StorageConfig {

	@Bean
	@ConditionalOnProperty(name = "platform.storage", havingValue = "jdbc", matchIfMissing = true)
	DocumentStore jdbcDocumentStore(JdbcClient jdbc, PlatformProperties properties) {
		return new JdbcDocumentStore(jdbc, properties);
	}

	@Bean
	@ConditionalOnProperty(name = "platform.storage", havingValue = "memory")
	DocumentStore inMemoryDocumentStore(PlatformProperties properties) {
		return new InMemoryDocumentStore(properties);
	}

	@Bean
	@ConditionalOnProperty(name = "platform.storage", havingValue = "memory")
	ChatMemoryRepository inMemoryChatMemoryRepository() {
		return new InMemoryChatMemoryRepository();
	}
}
