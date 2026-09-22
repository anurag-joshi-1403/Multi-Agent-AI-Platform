package com.project.multi_agent_ai_platform.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.anthropic.core.JsonValue;
import com.google.genai.errors.ApiException;
import com.anthropic.core.http.Headers;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.project.multi_agent_ai_platform.agent.core.Agent;
import com.project.multi_agent_ai_platform.agent.core.AgentOrchestrator;
import com.project.multi_agent_ai_platform.agent.core.AgentParameter;
import com.project.multi_agent_ai_platform.agent.core.AgentRegistry;
import com.project.multi_agent_ai_platform.agent.core.AgentRequest;
import com.project.multi_agent_ai_platform.agent.core.AgentResponse;
import com.project.multi_agent_ai_platform.agent.core.InvalidAgentRequestException;
import com.project.multi_agent_ai_platform.agent.core.UnknownAgentException;
import com.project.multi_agent_ai_platform.config.LlmProvider;
import com.project.multi_agent_ai_platform.config.PlatformProperties;
import com.project.multi_agent_ai_platform.config.SecurityConfig;

@WebMvcTest(AgentController.class)
@Import({ SecurityConfig.class, AgentControllerTest.ProviderConfig.class })
@EnableConfigurationProperties(PlatformProperties.class)
class AgentControllerTest {

	@TestConfiguration
	static class ProviderConfig {

		@Bean
		LlmProvider llmProvider() {
			return LlmProvider.GEMINI;
		}

		/** Every request in this test class runs as an authenticated user unless a test overrides
		 * it (e.g. to assert the 401 case) — the security boundary itself is covered once per
		 * controller rather than re-proven in every unrelated test. */
		@Bean
		MockMvcBuilderCustomizer authenticatedByDefault() {
			return builder -> builder.defaultRequest(MockMvcRequestBuilders.get("/").with(user("tester")));
		}
	}

	@Autowired
	MockMvc mvc;

	@MockitoBean
	AgentRegistry registry;

	@MockitoBean
	AgentOrchestrator orchestrator;

	private static Agent agent(String id, String description) {
		return new Agent() {
			@Override
			public String id() {
				return id;
			}

			@Override
			public String description() {
				return description;
			}

			@Override
			public List<String> capabilities() {
				return List.of("A", "B");
			}

			@Override
			public List<AgentParameter> parameters() {
				return List.of(AgentParameter.select("style", "Style", "how", List.of("a", "b"), "a"));
			}

			@Override
			public AgentResponse handle(AgentRequest request) {
				return AgentResponse.of(id, "unused");
			}
		};
	}

	@Test
	void unauthenticatedRequestIs401ProblemDetail() throws Exception {
		mvc.perform(get("/api/agents").with(anonymous()))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.title").value("Authentication required"));

		verifyNoInteractions(registry);
	}

	@Test
	void listsAgentsWhenAuthenticated() throws Exception {
		when(registry.all()).thenReturn(List.of(agent("coding", "writes code"), agent("general", "chat")));

		mvc.perform(get("/api/agents"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].id").value("coding"))
			.andExpect(jsonPath("$[0].name").value("Coding Agent"))
			.andExpect(jsonPath("$[0].description").value("writes code"))
			.andExpect(jsonPath("$[0].capabilities[1]").value("B"))
			.andExpect(jsonPath("$[0].parameters[0].name").value("style"))
			.andExpect(jsonPath("$[0].parameters[0].type").value("SELECT"))
			.andExpect(jsonPath("$[0].parameters[0].options[1]").value("b"))
			.andExpect(jsonPath("$[0].parameters[0].defaultValue").value("a"));
	}

	@Test
	void runsAgentAndReturnsResponseWithConversationId() throws Exception {
		when(orchestrator.dispatch(eq("coding"), any()))
			.thenReturn(new AgentResponse("coding", "here is code", Map.of("language", "java")));

		mvc.perform(post("/api/agents/coding/run")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"conversationId\":\"c-1\",\"message\":\"write code\",\"attributes\":{\"language\":\"java\"}}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.agentId").value("coding"))
			.andExpect(jsonPath("$.content").value("here is code"))
			.andExpect(jsonPath("$.metadata.language").value("java"))
			.andExpect(jsonPath("$.conversationId").value("c-1"))
			.andExpect(jsonPath("$.elapsedMs").isNumber());

		ArgumentCaptor<AgentRequest> captor = ArgumentCaptor.forClass(AgentRequest.class);
		verify(orchestrator).dispatch(eq("coding"), captor.capture());
		assertThat(captor.getValue().conversationId()).isEqualTo("c-1");
		assertThat(captor.getValue().attributes()).containsEntry("language", "java");
	}

	@Test
	void mintsConversationIdWhenOmitted() throws Exception {
		when(orchestrator.dispatch(eq("general"), any())).thenReturn(AgentResponse.of("general", "hi"));

		mvc.perform(post("/api/agents/general/run")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"message\":\"hello\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.conversationId").isNotEmpty());
	}

	@Test
	void blankMessageIsRejectedBeforeReachingTheOrchestrator() throws Exception {
		mvc.perform(post("/api/agents/general/run")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"message\":\"   \"}"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400));

		verifyNoInteractions(orchestrator);
	}

	@Test
	void unknownAgentIsProblemDetail404() throws Exception {
		when(orchestrator.dispatch(eq("nope"), any())).thenThrow(new UnknownAgentException("nope"));

		mvc.perform(post("/api/agents/nope/run")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"message\":\"hello\"}"))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Unknown agent"))
			.andExpect(jsonPath("$.detail").value("No agent registered with id 'nope'"))
			.andExpect(jsonPath("$.agentId").value("nope"));
	}

	@Test
	void invalidAgentRequestIs400() throws Exception {
		when(orchestrator.dispatch(eq("document"), any()))
			.thenThrow(new InvalidAgentRequestException("documentId is required"));

		mvc.perform(post("/api/agents/document/run")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"message\":\"what does it say?\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("documentId is required"));
	}

	@Test
	void geminiInvalidKeyIs502WithHint() throws Exception {
		when(orchestrator.dispatch(eq("general"), any()))
			.thenThrow(new ApiException(400, "INVALID_ARGUMENT", "API key not valid. Please pass a valid API key."));

		mvc.perform(post("/api/agents/general/run")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"message\":\"hello\"}"))
			.andExpect(status().isBadGateway())
			.andExpect(jsonPath("$.title").value("LLM provider rejected the credentials"))
			.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("GEMINI_API_KEY")));
	}

	@Test
	void geminiErrorWrappedBySpringAiIsStillMapped() throws Exception {
		when(orchestrator.dispatch(eq("general"), any()))
			.thenThrow(new RuntimeException("Failed to generate content",
					new ApiException(400, "INVALID_ARGUMENT", "API key not valid. Please pass a valid API key.")));

		mvc.perform(post("/api/agents/general/run")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"message\":\"hello\"}"))
			.andExpect(status().isBadGateway())
			.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("GEMINI_API_KEY")));
	}

	@Test
	void geminiServerErrorIs503() throws Exception {
		when(orchestrator.dispatch(eq("general"), any()))
			.thenThrow(new ApiException(503, "UNAVAILABLE", "The model is overloaded."));

		mvc.perform(post("/api/agents/general/run")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"message\":\"hello\"}"))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Google Gemini")));
	}

	@Test
	void anthropicAuthFailureIs502WithHint() throws Exception {
		when(orchestrator.dispatch(eq("general"), any()))
			.thenThrow(UnauthorizedException.builder().headers(Headers.builder().build()).body(JsonValue.from("invalid x-api-key")).build());

		mvc.perform(post("/api/agents/general/run")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"message\":\"hello\"}"))
			.andExpect(status().isBadGateway())
			.andExpect(jsonPath("$.title").value("LLM provider rejected the credentials"));
	}

	@Test
	void anthropicRateLimitIs503() throws Exception {
		when(orchestrator.dispatch(eq("general"), any()))
			.thenThrow(RateLimitException.builder().headers(Headers.builder().build()).body(JsonValue.from("rate limited")).build());

		mvc.perform(post("/api/agents/general/run")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"message\":\"hello\"}"))
			.andExpect(status().isServiceUnavailable())
			.andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("problem+json")));
	}
}
