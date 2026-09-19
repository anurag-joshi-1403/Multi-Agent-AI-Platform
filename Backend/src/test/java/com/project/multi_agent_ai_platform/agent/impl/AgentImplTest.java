package com.project.multi_agent_ai_platform.agent.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.project.multi_agent_ai_platform.agent.core.AgentRequest;
import com.project.multi_agent_ai_platform.agent.core.AgentResponse;
import com.project.multi_agent_ai_platform.agent.core.InvalidAgentRequestException;
import com.project.multi_agent_ai_platform.agent.llm.StubChatModel;
import com.project.multi_agent_ai_platform.config.PlatformProperties;
import com.project.multi_agent_ai_platform.document.DocumentNotFoundException;
import com.project.multi_agent_ai_platform.document.DocumentStore;
import com.project.multi_agent_ai_platform.document.StoredDocument;

/** Behaviour of each concrete agent on top of the stubbed model. */
class AgentImplTest {

	private final StubChatModel model = new StubChatModel();

	private static PlatformProperties properties(int maxContextChars) {
		return new PlatformProperties(new PlatformProperties.Cors(List.of()), new PlatformProperties.Memory(20),
				new PlatformProperties.Documents(maxContextChars, 50));
	}

	// --- coding -------------------------------------------------------------

	@Test
	void codingAgentDetectsLanguageFromFence() {
		model.reply = "```java\nclass A {}\n```\nDone.";
		CodingAgent agent = new CodingAgent(model.clientBuilder(), StubChatModel.memory());

		AgentResponse response = agent.handle(AgentRequest.of("write a class"));

		assertThat(response.agentId()).isEqualTo("coding");
		assertThat(response.metadata()).containsEntry("language", "java").containsEntry("model", "stub-model");
	}

	@Test
	void codingAgentHonoursLanguageAttribute() {
		CodingAgent agent = new CodingAgent(model.clientBuilder(), StubChatModel.memory());

		AgentResponse response = agent.handle(new AgentRequest(null, "sort a list", Map.of("language", "Go")));

		assertThat(model.lastPrompt().getUserMessage().getText()).startsWith("Target language: Go");
		assertThat(response.metadata()).containsEntry("language", "Go");
	}

	@Test
	void detectLanguageReturnsNullWithoutFence() {
		assertThat(CodingAgent.detectLanguage("no code here")).isNull();
	}

	// --- research -----------------------------------------------------------

	@Test
	void researchAgentExtractsConfidence() {
		model.reply = "**Summary** ...\n**Confidence** - medium, because ...";
		ResearchAgent agent = new ResearchAgent(model.clientBuilder(), StubChatModel.memory());

		AgentResponse response = agent.handle(AgentRequest.of("vector databases"));

		assertThat(response.metadata()).containsEntry("mode", "knowledge").containsEntry("confidence", "medium");
	}

	// --- summarizer ---------------------------------------------------------

	@Test
	void summarizerAppliesStyleAndReportsCompression() {
		model.reply = "TL;DR: short.";
		SummarizerAgent agent = new SummarizerAgent(model.clientBuilder(), StubChatModel.memory());
		String longText = "word ".repeat(200);

		AgentResponse response = agent.handle(new AgentRequest(null, longText, Map.of("style", "TL;DR", "maxWords", 40)));

		assertThat(model.lastPrompt().getUserMessage().getText())
			.startsWith("Write a single-paragraph TL;DR of at most 40 words.")
			.contains(longText.strip());
		assertThat(response.metadata())
			.containsEntry("style", "tldr")
			.containsEntry("maxWords", 40)
			.containsEntry("inputChars", longText.length());
		assertThat((Double) response.metadata().get("compressionRatio")).isLessThan(0.1);
	}

	@Test
	void summarizerNormalisesStyles() {
		assertThat(SummarizerAgent.normaliseStyle(null)).isEqualTo("bullets");
		assertThat(SummarizerAgent.normaliseStyle("Brief")).isEqualTo("executive");
		assertThat(SummarizerAgent.normaliseStyle("weird")).isEqualTo("bullets");
	}

	// --- document -----------------------------------------------------------

	@Test
	void documentAgentInjectsDocumentIntoSystemPromptOnly() {
		DocumentStore store = new DocumentStore(properties(60_000));
		StoredDocument doc = store.save("contract.pdf", "application/pdf", "[page 2]\nThe contract renews automatically.", 3);
		DocumentAgent agent = new DocumentAgent(model.clientBuilder(), StubChatModel.memory(), store,
				properties(60_000));

		AgentResponse response = agent.handle(new AgentRequest("c1", "Does it renew?", Map.of("documentId", doc.id())));

		assertThat(model.lastPrompt().getSystemMessage().getText())
			.contains("DOCUMENT: contract.pdf")
			.contains("The contract renews automatically.");
		assertThat(model.lastPrompt().getUserMessage().getText()).isEqualTo("Does it renew?");
		assertThat(response.metadata())
			.containsEntry("documentId", doc.id())
			.containsEntry("documentName", "contract.pdf")
			.containsEntry("pages", 3)
			.containsEntry("truncated", false);
	}

	@Test
	void documentAgentTruncatesLongDocuments() {
		DocumentStore store = new DocumentStore(properties(100));
		StoredDocument doc = store.save("big.txt", "text/plain", "x".repeat(1_000), null);
		DocumentAgent agent = new DocumentAgent(model.clientBuilder(), StubChatModel.memory(), store,
				properties(100));

		AgentResponse response = agent.handle(new AgentRequest(null, "q", Map.of("documentId", doc.id())));

		assertThat(response.metadata()).containsEntry("truncated", true).containsEntry("contextChars", 100);
		assertThat(model.lastPrompt().getSystemMessage().getText()).contains("(truncated)");
	}

	@Test
	void documentAgentRequiresDocumentId() {
		DocumentAgent agent = new DocumentAgent(model.clientBuilder(), StubChatModel.memory(),
				new DocumentStore(properties(100)), properties(100));

		assertThatThrownBy(() -> agent.handle(AgentRequest.of("q")))
			.isInstanceOf(InvalidAgentRequestException.class)
			.hasMessageContaining("documentId");
		assertThatThrownBy(() -> agent.handle(new AgentRequest(null, "q", Map.of("documentId", "doc_nope"))))
			.isInstanceOf(DocumentNotFoundException.class);
		assertThat(model.prompts).isEmpty();
	}

	// --- general ------------------------------------------------------------

	@Test
	void generalAgentPassesMessageThrough() {
		GeneralAgent agent = new GeneralAgent(model.clientBuilder(), StubChatModel.memory());

		AgentResponse response = agent.handle(AgentRequest.of("hello"));

		assertThat(response.agentId()).isEqualTo("general");
		assertThat(agent.name()).isEqualTo("General Assistant");
		assertThat(response.content()).isEqualTo("stub reply");
	}
}
