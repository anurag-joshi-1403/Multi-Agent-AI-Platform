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
import com.project.multi_agent_ai_platform.document.AttachmentResolver;
import com.project.multi_agent_ai_platform.document.DocumentStore;
import com.project.multi_agent_ai_platform.document.InMemoryDocumentStore;
import com.project.multi_agent_ai_platform.document.StoredDocument;

/** Behaviour of each concrete agent on top of the stubbed model. */
class AgentImplTest {

	private final StubChatModel model = new StubChatModel();

	private static PlatformProperties properties(int maxContextChars) {
		return new PlatformProperties(PlatformProperties.Storage.MEMORY, new PlatformProperties.Cors(List.of()), new PlatformProperties.Memory(20),
				new PlatformProperties.Documents(maxContextChars, 50), new PlatformProperties.Auth("tester:tester"));
	}

	private static AttachmentResolver resolver(DocumentStore store, int maxContextChars) {
		return new AttachmentResolver(store, properties(maxContextChars));
	}

	/** A resolver over an empty store, for agents under test that are not given attachments. */
	private static AttachmentResolver resolver() {
		return resolver(new InMemoryDocumentStore(properties(60_000)), 60_000);
	}

	private static Map<String, Object> attaching(String... documentIds) {
		return Map.of(AttachmentResolver.ATTRIBUTE, List.of(documentIds));
	}

	// --- coding -------------------------------------------------------------

	@Test
	void codingAgentDetectsLanguageFromFence() {
		model.reply = "```java\nclass A {}\n```\nDone.";
		CodingAgent agent = new CodingAgent(model.clientBuilder(), StubChatModel.memory(), resolver());

		AgentResponse response = agent.handle(AgentRequest.of("write a class"));

		assertThat(response.agentId()).isEqualTo("coding");
		assertThat(response.metadata()).containsEntry("language", "java").containsEntry("model", "stub-model");
	}

	@Test
	void codingAgentHonoursLanguageAttribute() {
		CodingAgent agent = new CodingAgent(model.clientBuilder(), StubChatModel.memory(), resolver());

		AgentResponse response = agent.handle(new AgentRequest(null, "sort a list", Map.of("language", "Go")));

		assertThat(model.lastPrompt().getUserMessage().getText())
			.startsWith("Answer in Go.")
			.contains("must be Go")
			.endsWith("sort a list");
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
		ResearchAgent agent = new ResearchAgent(model.clientBuilder(), StubChatModel.memory(), resolver());

		AgentResponse response = agent.handle(AgentRequest.of("vector databases"));

		assertThat(response.metadata()).containsEntry("mode", "knowledge").containsEntry("confidence", "medium");
	}

	// --- summarizer ---------------------------------------------------------

	@Test
	void summarizerAppliesStyleAndReportsCompression() {
		model.reply = "TL;DR: short.";
		SummarizerAgent agent = new SummarizerAgent(model.clientBuilder(), StubChatModel.memory(), resolver());
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

	// --- attachments (available to every agent) -----------------------------

	@Test
	void anyAgentSeesAttachedFilesWithoutLosingItsOwnSystemPrompt() {
		DocumentStore store = new InMemoryDocumentStore(properties(60_000));
		StoredDocument doc = store.save("notes.md", "text/markdown", "The build runs on Java 25.", null);
		CodingAgent agent = new CodingAgent(model.clientBuilder(), StubChatModel.memory(), resolver(store, 60_000));

		agent.handle(new AgentRequest("c1", "which java?", attaching(doc.id())));

		assertThat(model.lastPrompt().getSystemMessage().getText())
			.contains("You are a senior software engineer")
			.contains("FILE: notes.md")
			.contains("The build runs on Java 25.");
		assertThat(model.lastPrompt().getUserMessage().getText()).isEqualTo("which java?");
	}

	@Test
	void severalAttachmentsShareTheContextBudget() {
		DocumentStore store = new InMemoryDocumentStore(properties(60_000));
		StoredDocument a = store.save("a.txt", "text/plain", "alpha", null);
		StoredDocument b = store.save("b.txt", "text/plain", "beta", null);
		GeneralAgent agent = new GeneralAgent(model.clientBuilder(), StubChatModel.memory(), resolver(store, 60_000));

		agent.handle(new AgentRequest("c1", "compare", attaching(a.id(), b.id())));

		assertThat(model.lastPrompt().getSystemMessage().getText())
			.contains("FILE: a.txt")
			.contains("alpha")
			.contains("FILE: b.txt")
			.contains("beta");
	}

	@Test
	void evictedAttachmentIdsAreSkippedRatherThanFailingTheCall() {
		GeneralAgent agent = new GeneralAgent(model.clientBuilder(), StubChatModel.memory(),
				resolver(new InMemoryDocumentStore(properties(100)), 100));

		AgentResponse response = agent.handle(new AgentRequest(null, "hi", attaching("doc_gone")));

		assertThat(response.content()).isEqualTo("stub reply");
		assertThat(model.lastPrompt().getSystemMessage().getText()).doesNotContain("--- FILE:");
	}

	// --- document -----------------------------------------------------------

	@Test
	void documentAgentInjectsAttachmentIntoSystemPromptOnly() {
		DocumentStore store = new InMemoryDocumentStore(properties(60_000));
		StoredDocument doc = store.save("contract.pdf", "application/pdf", "[page 2]\nThe contract renews automatically.", 3);
		DocumentAgent agent = new DocumentAgent(model.clientBuilder(), StubChatModel.memory(), resolver(store, 60_000));

		AgentResponse response = agent.handle(new AgentRequest("c1", "Does it renew?", attaching(doc.id())));

		assertThat(model.lastPrompt().getSystemMessage().getText())
			.contains("FILE: contract.pdf")
			.contains("The contract renews automatically.");
		assertThat(model.lastPrompt().getUserMessage().getText()).isEqualTo("Does it renew?");
		assertThat(response.metadata())
			.containsEntry("documentIds", List.of(doc.id()))
			.containsEntry("documentNames", List.of("contract.pdf"));
	}

	@Test
	void documentAgentTruncatesLongAttachments() {
		DocumentStore store = new InMemoryDocumentStore(properties(100));
		StoredDocument doc = store.save("big.txt", "text/plain", "x".repeat(1_000), null);
		DocumentAgent agent = new DocumentAgent(model.clientBuilder(), StubChatModel.memory(), resolver(store, 100));

		agent.handle(new AgentRequest(null, "q", attaching(doc.id())));

		assertThat(model.lastPrompt().getSystemMessage().getText())
			.contains("big.txt (truncated)")
			.doesNotContain("x".repeat(101));
	}

	@Test
	void documentAgentRequiresAnAttachment() {
		DocumentAgent agent = new DocumentAgent(model.clientBuilder(), StubChatModel.memory(),
				resolver(new InMemoryDocumentStore(properties(100)), 100));

		assertThatThrownBy(() -> agent.handle(AgentRequest.of("q")))
			.isInstanceOf(InvalidAgentRequestException.class)
			.hasMessageContaining("attached file");
		assertThatThrownBy(() -> agent.handle(new AgentRequest(null, "q", attaching("doc_nope"))))
			.isInstanceOf(InvalidAgentRequestException.class);
		assertThat(model.prompts).isEmpty();
	}

	@Test
	void documentAgentNoLongerAsksForAPickerParameter() {
		DocumentAgent agent = new DocumentAgent(model.clientBuilder(), StubChatModel.memory(), resolver());

		assertThat(agent.parameters()).isEmpty();
	}

	// --- general ------------------------------------------------------------

	@Test
	void generalAgentPassesMessageThrough() {
		GeneralAgent agent = new GeneralAgent(model.clientBuilder(), StubChatModel.memory(), resolver());

		AgentResponse response = agent.handle(AgentRequest.of("hello"));

		assertThat(response.agentId()).isEqualTo("general");
		assertThat(agent.name()).isEqualTo("General Assistant");
		assertThat(response.content()).isEqualTo("stub reply");
	}
}
