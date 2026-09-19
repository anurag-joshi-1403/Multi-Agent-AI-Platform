package com.project.multi_agent_ai_platform.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.multi_agent_ai_platform.agent.core.AgentOrchestrator;
import com.project.multi_agent_ai_platform.agent.core.AgentRegistry;
import com.project.multi_agent_ai_platform.agent.core.AgentResponse;
import com.project.multi_agent_ai_platform.web.dto.AgentSummary;
import com.project.multi_agent_ai_platform.web.dto.RunAgentRequest;
import com.project.multi_agent_ai_platform.web.dto.RunAgentResponse;

import jakarta.validation.Valid;

/**
 * The agent API the frontend talks to.
 * <pre>
 *   GET  /api/agents           list registered agents
 *   GET  /api/agents/{id}      one agent
 *   POST /api/agents/{id}/run  run an agent with a message
 * </pre>
 */
@RestController
@RequestMapping(path = "/api/agents", produces = MediaType.APPLICATION_JSON_VALUE)
public class AgentController {

	private final AgentRegistry registry;

	private final AgentOrchestrator orchestrator;

	public AgentController(AgentRegistry registry, AgentOrchestrator orchestrator) {
		this.registry = registry;
		this.orchestrator = orchestrator;
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
		String conversationId = body.conversationId() == null || body.conversationId().isBlank()
				? UUID.randomUUID().toString()
				: body.conversationId();

		long started = System.nanoTime();
		AgentResponse response = orchestrator.dispatch(id, body.toAgentRequest(conversationId));
		long elapsedMs = (System.nanoTime() - started) / 1_000_000;

		return RunAgentResponse.of(response, conversationId, elapsedMs);
	}
}
