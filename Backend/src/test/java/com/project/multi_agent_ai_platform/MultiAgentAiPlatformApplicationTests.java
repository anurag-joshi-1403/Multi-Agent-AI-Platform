package com.project.multi_agent_ai_platform;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.project.multi_agent_ai_platform.agent.core.Agent;
import com.project.multi_agent_ai_platform.agent.core.AgentRegistry;
import com.project.multi_agent_ai_platform.config.LlmProvider;
import com.project.multi_agent_ai_platform.config.PlatformProperties;

@SpringBootTest
class MultiAgentAiPlatformApplicationTests {

	@Autowired
	AgentRegistry registry;

	@Autowired
	LlmProvider provider;

	@Autowired
	PlatformProperties properties;

	@Test
	void contextLoadsAndDiscoversTheAgents() {
		assertThat(registry.all()).extracting(Agent::id)
			.containsExactlyInAnyOrder("coding", "general", "research", "summarizer");
	}

	@Test
	void memoryWindowDefaultsTo20() {
		assertThat(properties.memory().maxMessages()).isEqualTo(20);
	}

	@Test
	void testsUseTheTestConfigNeverBackendEnv() {
		// gemini-test only exists in src/test/resources/application.properties
		assertThat(provider.model()).isEqualTo("gemini-test");
	}
}
