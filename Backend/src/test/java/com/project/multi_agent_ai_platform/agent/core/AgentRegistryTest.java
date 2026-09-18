package com.project.multi_agent_ai_platform.agent.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class AgentRegistryTest {

	@Test
	void looksUpAgentsById() {
		AgentRegistry registry = new AgentRegistry(List.of(new StubAgent("coding"), new StubAgent("pdf")));

		assertThat(registry.get("coding").id()).isEqualTo("coding");
		assertThat(registry.contains("pdf")).isTrue();
		assertThat(registry.all()).extracting(Agent::id).containsExactly("coding", "pdf");
	}

	@Test
	void unknownIdThrows() {
		AgentRegistry registry = new AgentRegistry(List.of(new StubAgent("coding")));

		assertThatThrownBy(() -> registry.get("nope"))
				.isInstanceOf(UnknownAgentException.class)
				.hasMessageContaining("nope");
		assertThat(registry.contains("nope")).isFalse();
	}

	@Test
	void duplicateIdsFailAtConstruction() {
		assertThatThrownBy(() -> new AgentRegistry(List.of(new StubAgent("coding"), new StubAgent("coding"))))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Duplicate agent id 'coding'");
	}

	@Test
	void emptyRegistryIsAllowed() {
		AgentRegistry registry = new AgentRegistry(List.of());

		assertThat(registry.all()).isEmpty();
	}
}
