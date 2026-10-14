package com.project.multi_agent_ai_platform.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.multi_agent_ai_platform.agent.core.AgentEvent;
import com.project.multi_agent_ai_platform.agent.core.AgentOrchestrator;
import com.project.multi_agent_ai_platform.agent.core.AgentRegistry;
import com.project.multi_agent_ai_platform.agent.core.AgentResponse;
import com.project.multi_agent_ai_platform.web.dto.AgentSummary;
import com.project.multi_agent_ai_platform.web.dto.RunAgentRequest;
import com.project.multi_agent_ai_platform.web.dto.RunAgentResponse;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;

/**
 * The agent API the console talks to.
 * <pre>
 *   GET  /api/agents              list registered agents (also the console's "Backend online" check)
 *   GET  /api/agents/{id}         one agent
 *   POST /api/agents/{id}/run     run an agent with a message
 *   POST /api/agents/{id}/stream  the same, the reply sent as it is written (server-sent events)
 * </pre>
 */
@RestController
@RequestMapping(path = "/api/agents", produces = MediaType.APPLICATION_JSON_VALUE)
public class AgentController {

	private final AgentRegistry registry;

	private final AgentOrchestrator orchestrator;

	private final ApiExceptionHandler errors;

	public AgentController(AgentRegistry registry, AgentOrchestrator orchestrator, ApiExceptionHandler errors) {
		this.registry = registry;
		this.orchestrator = orchestrator;
		this.errors = errors;
	}

	@GetMapping
	public List<AgentSummary> list() {
		return registry.all().stream().map(AgentSummary::of).toList();
	}

	@GetMapping("/{id}")
	public AgentSummary get(@PathVariable String id) {
		return AgentSummary.of(registry.get(id));
	}

	@PostMapping(path = "/{id}/run", consumes = MediaType.APPLICATION_JSON_VALUE)
	public RunAgentResponse run(@PathVariable String id, @Valid @RequestBody RunAgentRequest body) {
		String conversationId = conversationId(body);

		long started = System.nanoTime();
		AgentResponse response = orchestrator.dispatch(id, body.toAgentRequest(conversationId));
		long elapsedMs = (System.nanoTime() - started) / 1_000_000;

		return RunAgentResponse.of(response, conversationId, elapsedMs);
	}

	/**
	 * As {@code /run}, with the reply sent as it is written: a {@code chunk} event
	 * {@code {"text": "..."}} for each piece, then one {@code done} event with the body {@code /run}
	 * returns. A problem with the request (unknown agent, invalid body, no file for the Document
	 * Agent) is an ordinary error response; a provider failure once the stream is open arrives as an
	 * {@code error} event with the problem+json body, and ends the stream.
	 */
	@PostMapping(path = "/{id}/stream", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<Object>> stream(@PathVariable String id, @Valid @RequestBody RunAgentRequest body) {
		String conversationId = conversationId(body);
		long started = System.nanoTime();
		return orchestrator.stream(id, body.toAgentRequest(conversationId))
			.map(event -> switch (event) {
				case AgentEvent.Chunk chunk -> sse("chunk", Map.of("text", chunk.text()));
				case AgentEvent.Done done -> sse("done", RunAgentResponse.of(done.response(), conversationId,
						(System.nanoTime() - started) / 1_000_000));
			})
			.onErrorResume(ex -> Flux.just(sse("error", errors.problemFor(ex))));
	}

	private static ServerSentEvent<Object> sse(String event, Object data) {
		return ServerSentEvent.<Object>builder(data).event(event).build();
	}

	/** The request's conversation, or a new one when it has none. */
	private static String conversationId(RunAgentRequest body) {
		return body.conversationId() == null || body.conversationId().isBlank() ? UUID.randomUUID().toString()
				: body.conversationId();
	}
}
