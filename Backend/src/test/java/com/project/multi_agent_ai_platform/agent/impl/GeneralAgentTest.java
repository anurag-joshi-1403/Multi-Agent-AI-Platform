package com.project.multi_agent_ai_platform.agent.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.project.multi_agent_ai_platform.agent.core.AgentRequest;
import com.project.multi_agent_ai_platform.agent.core.AgentResponse;
import com.project.multi_agent_ai_platform.agent.llm.StubChatModel;

class GeneralAgentTest {

	private final StubChatModel model = new StubChatModel();

	private final GeneralAgent agent = new GeneralAgent(model.clientBuilder(), StubChatModel.memory(),
			StubChatModel.noAttachments(), StubChatModel.provider());

	@Test
	void describesItselfTheWayTheConsoleExpects() {
		assertThat(agent.id()).isEqualTo("general");
		assertThat(agent.name()).isEqualTo("General Assistant");
		assertThat(agent.description()).isNotBlank();
		assertThat(agent.capabilities()).containsExactly("Chat", "Brainstorm", "Drafting");
		assertThat(agent.parameters()).isEmpty();
	}

	@Test
	void answersWithItsSystemPrompt() {
		model.reply = "Paris.";

		AgentResponse response = agent.handle(AgentRequest.of("Capital of France?"));

		assertThat(response.agentId()).isEqualTo("general");
		assertThat(response.content()).isEqualTo("Paris.");
		assertThat(model.lastPrompt().getSystemMessage().getText()).isEqualTo(GeneralAgent.SYSTEM_PROMPT);
		assertThat(model.lastPrompt().getUserMessage().getText()).isEqualTo("Capital of France?");
	}
}
