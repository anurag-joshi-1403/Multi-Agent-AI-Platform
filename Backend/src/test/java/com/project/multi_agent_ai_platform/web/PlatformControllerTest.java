package com.project.multi_agent_ai_platform.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.project.multi_agent_ai_platform.agent.core.Agent;
import com.project.multi_agent_ai_platform.agent.core.AgentRegistry;
import com.project.multi_agent_ai_platform.agent.core.AgentRequest;
import com.project.multi_agent_ai_platform.agent.core.AgentResponse;
import com.project.multi_agent_ai_platform.config.LlmProvider;
import com.project.multi_agent_ai_platform.config.LlmProviderInfo;
import com.project.multi_agent_ai_platform.config.PlatformProperties;
import com.project.multi_agent_ai_platform.config.SecurityConfig;
import com.project.multi_agent_ai_platform.document.InMemoryDocumentStore;

@WebMvcTest(PlatformController.class)
@Import({ SecurityConfig.class, InMemoryDocumentStore.class, PlatformControllerTest.ProviderConfig.class })
@EnableConfigurationProperties(PlatformProperties.class)
class PlatformControllerTest {

	@TestConfiguration
	static class ProviderConfig {

		@Bean
		LlmProvider llmProvider() {
			return LlmProvider.GEMINI;
		}

		@Bean
		LlmProviderInfo llmProviderInfo() {
			return new LlmProviderInfo(LlmProvider.GEMINI, "gemini-2.5-flash", false);
		}

		@Bean
		MockMvcBuilderCustomizer authenticatedByDefault() {
			return builder -> builder.defaultRequest(MockMvcRequestBuilders.get("/").with(user("tester")));
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
			.andExpect(jsonPath("$.model").value("gemini-2.5-flash"))
			.andExpect(jsonPath("$.apiKeyConfigured").value(false))
			.andExpect(jsonPath("$.keyEnvVar").value("GEMINI_API_KEY"))
			.andExpect(jsonPath("$.agents").value(1))
			.andExpect(jsonPath("$.memoryMaxMessages").value(20))
			.andExpect(jsonPath("$.documents.stored").value(0))
			.andExpect(jsonPath("$.documents.maxStored").value(50))
			.andExpect(jsonPath("$.apiKey").doesNotExist());
	}

	@Test
	void unauthenticatedRequestIs401() throws Exception {
		mvc.perform(get("/api/platform").with(anonymous())).andExpect(status().isUnauthorized());
	}
}
