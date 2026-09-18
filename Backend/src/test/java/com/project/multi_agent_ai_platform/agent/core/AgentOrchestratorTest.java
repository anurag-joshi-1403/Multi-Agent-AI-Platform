package com.project.multi_agent_ai_platform.agent.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class AgentOrchestratorTest {

	private final AgentOrchestrator orchestrator = new AgentOrchestrator(
			new AgentRegistry(List.of(new StubAgent("coding"))));

	@Test
	void dispatchesToNamedAgent() {
		AgentResponse response = orchestrator.dispatch("coding", AgentRequest.of("hello"));

		assertThat(response.agentId()).isEqualTo("coding");
		assertThat(response.content()).isEqualTo("echo:hello");
	}

	@Test
	void unknownAgentPropagates() {
		assertThatThrownBy(() -> orchestrator.dispatch("search", AgentRequest.of("hello")))
				.isInstanceOf(UnknownAgentException.class);
	}
}
