package com.project.multi_agent_ai_platform;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.project.multi_agent_ai_platform.agent.core.Agent;
import com.project.multi_agent_ai_platform.agent.core.AgentRegistry;
import com.project.multi_agent_ai_platform.config.LlmProvider;

@SpringBootTest
class MultiAgentAiPlatformApplicationTests {

	@Autowired
	AgentRegistry registry;

	@Autowired
	LlmProvider provider;

	@Test
	void contextLoadsAndDiscoversTheAgents() {
		assertThat(registry.all()).extracting(Agent::id).containsExactly("general");
	}

	@Test
	void testsUseTheTestConfigNeverBackendEnv() {
		// gemini-test only exists in src/test/resources/application.properties
		assertThat(provider.model()).isEqualTo("gemini-test");
	}
}
