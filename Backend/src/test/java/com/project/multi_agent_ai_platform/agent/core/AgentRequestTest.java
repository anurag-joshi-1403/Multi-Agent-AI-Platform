package com.project.multi_agent_ai_platform.agent.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AgentRequestTest {

	@Test
	void rejectsBlankMessage() {
		assertThatThrownBy(() -> AgentRequest.of("   ")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new AgentRequest(null, null, null)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void nullAttributesBecomeEmptyAndAreCopied() {
		assertThat(new AgentRequest("c1", "hi", null).attributes()).isEmpty();

		Map<String, Object> mutable = new HashMap<>(Map.of("k", "v"));
		AgentRequest request = new AgentRequest("c1", "hi", mutable);
		mutable.put("k2", "v2");

		assertThat(request.attributes()).containsOnlyKeys("k");
	}
}
