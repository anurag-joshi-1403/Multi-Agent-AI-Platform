package com.project.multi_agent_ai_platform.agent.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.project.multi_agent_ai_platform.agent.core.AgentParameter;
import com.project.multi_agent_ai_platform.agent.core.AgentRequest;
import com.project.multi_agent_ai_platform.agent.core.AgentResponse;
import com.project.multi_agent_ai_platform.agent.llm.StubChatModel;
import com.project.multi_agent_ai_platform.document.FakeDocumentStore;
import com.project.multi_agent_ai_platform.document.StoredDocument;

class SummarizerAgentTest {

	private static final String TEXT = "The quarterly report shows revenue up 12% and costs flat. ".repeat(4);

	private final StubChatModel model = new StubChatModel();

	private final FakeDocumentStore store = new FakeDocumentStore();

	private final SummarizerAgent agent = new SummarizerAgent(model.clientBuilder(), StubChatModel.memory(),
			StubChatModel.attachments(store));

	@Test
	void describesItselfWithStyleAndMaxWords() {
		assertThat(agent.id()).isEqualTo("summarizer");
		assertThat(agent.name()).isEqualTo("Summarizer Agent");
		assertThat(agent.parameters()).extracting(AgentParameter::name).containsExactly("style", "maxWords");

		AgentParameter style = agent.parameters().get(0);
		assertThat(style.type()).isEqualTo(AgentParameter.Type.SELECT);
		assertThat(style.options()).containsExactly("bullets", "tldr", "executive");
		assertThat(style.defaultValue()).isEqualTo("bullets");

		AgentParameter maxWords = agent.parameters().get(1);
		assertThat(maxWords.type()).isEqualTo(AgentParameter.Type.NUMBER);
		assertThat(maxWords.defaultValue()).isEqualTo(150);
	}

	@Test
	void defaultsToBulletsOf150WordsAndQuotesTheInput() {
		model.reply = "- revenue up 12%";

		AgentResponse response = agent.handle(AgentRequest.of(TEXT));

		String sent = model.lastPrompt().getUserMessage().getText();
		assertThat(sent).startsWith("Write 3-7 bullet points").contains("At most 150 words").contains(TEXT.strip());
		assertThat(response.metadata())
			.containsEntry("style", "bullets")
			.containsEntry("maxWords", 150)
			.containsEntry("inputChars", TEXT.length())
			.containsKey("compressionRatio");
	}

	@Test
	void honoursStyleAndMaxWords() {
		agent.handle(new AgentRequest(null, TEXT, Map.of("style", "tldr", "maxWords", "60")));
		assertThat(model.lastPrompt().getUserMessage().getText()).startsWith("Write a single-paragraph TL;DR of at most 60 words.");

		agent.handle(new AgentRequest(null, TEXT, Map.of("style", "executive", "maxWords", 80)));
		assertThat(model.lastPrompt().getUserMessage().getText()).startsWith("Write an executive brief").contains("At most 80 words");
	}

	@Test
	void maxWordsHasAFloor() {
		AgentResponse response = agent.handle(new AgentRequest(null, TEXT, Map.of("maxWords", 3)));

		assertThat(response.metadata()).containsEntry("maxWords", SummarizerAgent.MIN_WORDS);
	}

	@Test
	void withAFileAttachedTheFileIsTheInputAndTheMessageSteersIt() {
		StoredDocument doc = store.save("report.txt", "text/plain", TEXT, null);

		AgentResponse response = agent.handle(new AgentRequest(null, "focus on revenue",
				Map.of("attachments", List.of(doc.id()), "style", "tldr")));

		String sent = model.lastPrompt().getUserMessage().getText();
		assertThat(sent).startsWith("Write a single-paragraph TL;DR")
			.contains("Summarise the attached file(s)")
			.contains("focus on revenue")
			.doesNotContain("Text to summarise");
		assertThat(model.lastPrompt().getSystemMessage().getText()).contains("--- FILE: report.txt ---");
		assertThat(response.metadata()).containsEntry("inputChars", TEXT.length());
	}

	@Test
	void unknownOrAliasedStylesAreNormalised() {
		assertThat(SummarizerAgent.normaliseStyle(null)).isEqualTo("bullets");
		assertThat(SummarizerAgent.normaliseStyle("poem")).isEqualTo("bullets");
		assertThat(SummarizerAgent.normaliseStyle("TL;DR")).isEqualTo("tldr");
		assertThat(SummarizerAgent.normaliseStyle("Brief")).isEqualTo("executive");
	}
}
