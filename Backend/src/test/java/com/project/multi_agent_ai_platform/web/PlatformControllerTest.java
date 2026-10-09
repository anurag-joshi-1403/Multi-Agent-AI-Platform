package com.project.multi_agent_ai_platform.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.project.multi_agent_ai_platform.agent.core.Agent;
import com.project.multi_agent_ai_platform.agent.core.AgentRegistry;
import com.project.multi_agent_ai_platform.agent.core.AgentRequest;
import com.project.multi_agent_ai_platform.agent.core.AgentResponse;
import com.project.multi_agent_ai_platform.config.LlmProvider;

@WebMvcTest(PlatformController.class)
@Import(PlatformControllerTest.ProviderConfig.class)
class PlatformControllerTest {

	private static final String KEY = "very-secret-key-value";

	@TestConfiguration
	static class ProviderConfig {

		@Bean
		LlmProvider llmProvider() {
			return LlmProvider.gemini("gemini-test", KEY);
		}
	}

	@Autowired
	MockMvc mvc;

	@MockitoBean
	AgentRegistry registry;

	@Test
	void reportsProviderModelAndKeyStateWithoutTheKey() throws Exception {
		Agent stub = new Agent() {
			@Override
			public String id() {
				return "general";
			}

			@Override
			public String description() {
				return "x";
			}

			@Override
			public AgentResponse handle(AgentRequest request) {
				return AgentResponse.of("general", "x");
			}
		};
		when(registry.all()).thenReturn(List.of(stub));

		mvc.perform(get("/api/platform"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.provider").value("google-genai"))
			.andExpect(jsonPath("$.providerName").value("Google Gemini"))
			.andExpect(jsonPath("$.model").value("gemini-test"))
			.andExpect(jsonPath("$.apiKeyConfigured").value(true))
			.andExpect(jsonPath("$.keyEnvVar").value("GEMINI_API_KEY"))
			.andExpect(jsonPath("$.agents").value(1))
			.andExpect(jsonPath("$.memoryMaxMessages").value(0))
			.andExpect(jsonPath("$.documents.stored").value(0))
			.andExpect(jsonPath("$.documents.maxStored").value(0))
			.andExpect(jsonPath("$.documents.maxContextChars").value(0))
			.andExpect(content().string(Matchers.not(Matchers.containsString(KEY))));
	}
}
