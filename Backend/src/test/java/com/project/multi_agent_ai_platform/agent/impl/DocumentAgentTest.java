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
import com.project.multi_agent_ai_platform.document.FakeDocumentStore;
import com.project.multi_agent_ai_platform.document.StoredDocument;

class DocumentAgentTest {

	private final StubChatModel model = new StubChatModel();

	private final FakeDocumentStore store = new FakeDocumentStore();

	private final DocumentAgent agent = new DocumentAgent(model.clientBuilder(), StubChatModel.memory(),
			StubChatModel.attachments(store), StubChatModel.provider());

	@Test
	void describesItself() {
		assertThat(agent.id()).isEqualTo("document");
		assertThat(agent.name()).isEqualTo("Document Agent");
		assertThat(agent.capabilities()).containsExactly("PDF Q&A", "Extraction", "Grounded answers");
		assertThat(agent.parameters()).isEmpty();
	}

	@Test
	void refusesToAnswerWithoutAFile() {
		assertThatThrownBy(() -> agent.handle(AgentRequest.of("what does it say?")))
			.isInstanceOf(InvalidAgentRequestException.class)
			.hasMessageContaining("attached file");
		// a stale id the store no longer holds counts as no file
		assertThatThrownBy(() -> agent.handle(new AgentRequest(null, "q", Map.of("attachments", List.of("doc_gone")))))
			.isInstanceOf(InvalidAgentRequestException.class);
		assertThat(model.prompts).isEmpty();
	}

	@Test
	void answersFromTheAttachedFile() {
		StoredDocument doc = store.save("contract.pdf", "application/pdf", "[page 2]\nIt renews every May.", 2);
		model.reply = "> It renews every May. (page 2)";

		AgentResponse response = agent.handle(new AgentRequest(null, "When does it renew?",
				Map.of("attachments", List.of(doc.id()))));

		assertThat(model.lastPrompt().getSystemMessage().getText())
			.startsWith(DocumentAgent.SYSTEM_PROMPT)
			.contains("--- FILE: contract.pdf ---")
			.contains("It renews every May.");
		assertThat(response.content()).isEqualTo("> It renews every May. (page 2)");
		assertThat(response.metadata())
			.containsEntry("documentIds", List.of(doc.id()))
			.containsEntry("documentNames", List.of("contract.pdf"));
	}
}
