package com.project.multi_agent_ai_platform.agent.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.project.multi_agent_ai_platform.agent.core.AgentRequest;
import com.project.multi_agent_ai_platform.agent.core.AgentResponse;
import com.project.multi_agent_ai_platform.agent.llm.StubChatModel;

class ResearchAgentTest {

	private final StubChatModel model = new StubChatModel();

	private final ResearchAgent agent = new ResearchAgent(model.clientBuilder(), StubChatModel.memory(),
			StubChatModel.noAttachments());

	@Test
	void describesItself() {
		assertThat(agent.id()).isEqualTo("research");
		assertThat(agent.name()).isEqualTo("Research Agent");
		assertThat(agent.capabilities()).containsExactly("Analysis", "Findings", "Confidence");
		assertThat(agent.parameters()).isEmpty();
	}

	@Test
	void reportsModeAndConfidence() {
		model.reply = """
				**Summary** Short.
				**Key findings**
				- one
				**Open questions** none
				**Confidence** - medium, because sources disagree.
				""";

		AgentResponse response = agent.handle(AgentRequest.of("Why is the sky blue?"));

		assertThat(model.lastPrompt().getSystemMessage().getText()).isEqualTo(ResearchAgent.SYSTEM_PROMPT);
		assertThat(response.metadata()).containsEntry("mode", "knowledge").containsEntry("confidence", "medium");
	}

	@Test
	void confidenceIsTheLevelRightAfterTheHeading() {
		assertThat(ResearchAgent.extractConfidence("**Confidence:** High")).isEqualTo("high");
		assertThat(ResearchAgent.extractConfidence("### Confidence\nLow - little data")).isEqualTo("low");
		assertThat(ResearchAgent.extractConfidence("Confidence level: medium")).isEqualTo("medium");
		assertThat(ResearchAgent.extractConfidence("**Confidence** — medium, not high")).isEqualTo("medium");
	}

	@Test
	void theClosingConfidenceSectionWins() {
		String reply = "My confidence high-level summary...\n**Confidence** - low";

		assertThat(ResearchAgent.extractConfidence(reply)).isEqualTo("low");
	}

	@Test
	void noConfidenceSectionMeansNoConfidence() {
		model.reply = "I do not know.";

		assertThat(agent.handle(AgentRequest.of("?")).metadata()).doesNotContainKey("confidence");
		assertThat(ResearchAgent.extractConfidence("high confidence in nothing")).isNull();
	}
}
