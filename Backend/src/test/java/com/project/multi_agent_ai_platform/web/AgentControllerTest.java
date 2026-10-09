package com.project.multi_agent_ai_platform.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.project.multi_agent_ai_platform.agent.core.Agent;
import com.project.multi_agent_ai_platform.agent.core.AgentOrchestrator;
import com.project.multi_agent_ai_platform.agent.core.AgentParameter;
import com.project.multi_agent_ai_platform.agent.core.AgentRegistry;
import com.project.multi_agent_ai_platform.agent.core.AgentRequest;
import com.project.multi_agent_ai_platform.agent.core.AgentResponse;
import com.project.multi_agent_ai_platform.agent.core.UnknownAgentException;
import com.project.multi_agent_ai_platform.config.LlmProvider;

@WebMvcTest(AgentController.class)
@Import(AgentControllerTest.ProviderConfig.class)
class AgentControllerTest {

	@TestConfiguration
	static class ProviderConfig {

		@Bean
		LlmProvider llmProvider() {
			return LlmProvider.gemini("gemini-test", "test-key-not-used");
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

	private ResultActions run(String agentId, String json) throws Exception {
		return mvc.perform(post("/api/agents/" + agentId + "/run").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	// --- happy paths -------------------------------------------------------------------------

	@Test
	void listsAgentsInTheShapeTheConsoleExpects() throws Exception {
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
			.andExpect(jsonPath("$[0].parameters[0].label").value("Style"))
			.andExpect(jsonPath("$[0].parameters[0].type").value("SELECT"))
			.andExpect(jsonPath("$[0].parameters[0].required").value(false))
			.andExpect(jsonPath("$[0].parameters[0].options[1]").value("b"))
			.andExpect(jsonPath("$[0].parameters[0].defaultValue").value("a"));
	}

	@Test
	void getsOneAgent() throws Exception {
		when(registry.get("general")).thenReturn(agent("general", "chat"));

		mvc.perform(get("/api/agents/general"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value("general"));
	}

	@Test
	void runsAgentAndReturnsResponseWithConversationId() throws Exception {
		when(orchestrator.dispatch(eq("coding"), any()))
			.thenReturn(new AgentResponse("coding", "here is code", Map.of("model", "gemini-test")));

		run("coding", "{\"conversationId\":\"c-1\",\"message\":\"write code\",\"attributes\":{\"language\":\"Java\"}}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.agentId").value("coding"))
			.andExpect(jsonPath("$.content").value("here is code"))
			.andExpect(jsonPath("$.metadata.model").value("gemini-test"))
			.andExpect(jsonPath("$.conversationId").value("c-1"))
			.andExpect(jsonPath("$.elapsedMs").isNumber());

		ArgumentCaptor<AgentRequest> captor = ArgumentCaptor.forClass(AgentRequest.class);
		verify(orchestrator).dispatch(eq("coding"), captor.capture());
		assertThat(captor.getValue().conversationId()).isEqualTo("c-1");
		assertThat(captor.getValue().attributes()).containsEntry("language", "Java");
	}

	@Test
	void mintsConversationIdWhenOmitted() throws Exception {
		when(orchestrator.dispatch(eq("general"), any())).thenReturn(AgentResponse.of("general", "hi"));

		run("general", "{\"message\":\"hello\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.conversationId").isNotEmpty());
	}

	// --- errors are always problem+json ------------------------------------------------------

	@Test
	void blankMessageIsRejectedBeforeReachingTheOrchestrator() throws Exception {
		run("general", "{\"message\":\"   \"}")
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

		verifyNoInteractions(orchestrator);
	}

	@Test
	void malformedJsonIs400ProblemDetail() throws Exception {
		run("general", "{not json")
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void unknownAgentIs404ProblemDetail() throws Exception {
		when(orchestrator.dispatch(eq("nope"), any())).thenThrow(new UnknownAgentException("nope"));

		run("nope", "{\"message\":\"hello\"}")
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Unknown agent"))
			.andExpect(jsonPath("$.detail").value("No agent registered with id 'nope'"))
			.andExpect(jsonPath("$.agentId").value("nope"));
	}

	@Test
	void geminiInvalidKeyIs502WithHint() throws Exception {
		when(orchestrator.dispatch(eq("general"), any()))
			.thenThrow(new ApiException(400, "INVALID_ARGUMENT", "API key not valid. Please pass a valid API key."));

		run("general", "{\"message\":\"hello\"}")
			.andExpect(status().isBadGateway())
			.andExpect(jsonPath("$.title").value("LLM provider rejected the credentials"))
			.andExpect(jsonPath("$.detail").value(containsString("GEMINI_API_KEY")));
	}

	@Test
	void geminiErrorWrappedBySpringAiIsStillMapped() throws Exception {
		when(orchestrator.dispatch(eq("general"), any()))
			.thenThrow(new RuntimeException("Failed to generate content",
					new ApiException(400, "INVALID_ARGUMENT", "API key not valid. Please pass a valid API key.")));

		run("general", "{\"message\":\"hello\"}")
			.andExpect(status().isBadGateway())
			.andExpect(jsonPath("$.detail").value(containsString("GEMINI_API_KEY")));
	}

	@Test
	void geminiOverloadedOrRateLimitedIs503() throws Exception {
		when(orchestrator.dispatch(eq("general"), any()))
			.thenThrow(new ApiException(429, "RESOURCE_EXHAUSTED", "Quota exceeded."));

		run("general", "{\"message\":\"hello\"}")
			.andExpect(status().isServiceUnavailable())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value(containsString("Google Gemini")));
	}

	@Test
	void geminiOtherRejectionIs502WithItsFirstLine() throws Exception {
		when(orchestrator.dispatch(eq("general"), any()))
			.thenThrow(new ApiException(404, "NOT_FOUND", "models/nope is not found.\nmore detail"));

		run("general", "{\"message\":\"hello\"}")
			.andExpect(status().isBadGateway())
			.andExpect(jsonPath("$.detail").value("Google Gemini returned HTTP 404: models/nope is not found."));
	}

	@Test
	void geminiUnreachableIs502() throws Exception {
		when(orchestrator.dispatch(eq("general"), any()))
			.thenThrow(new GenAiIOException("connect timed out"));

		run("general", "{\"message\":\"hello\"}")
			.andExpect(status().isBadGateway())
			.andExpect(jsonPath("$.title").value("LLM provider unreachable"));
	}

	@Test
	void unexpectedErrorIs500ProblemDetailWithoutInternals() throws Exception {
		when(orchestrator.dispatch(eq("general"), any())).thenThrow(new IllegalStateException("secret internals"));

		run("general", "{\"message\":\"hello\"}")
			.andExpect(status().isInternalServerError())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value(containsString("Check the backend logs")))
			.andExpect(content().string(org.hamcrest.Matchers.not(containsString("secret internals"))));
	}
}
