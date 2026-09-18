package com.project.multi_agent_ai_platform.agent.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Single entry point for running an agent.
 * <p>
 * Phase 2 routing is client-driven: the caller names the agent. Rule-based or
 * LLM-based routing slots in here later without changing callers or agents.
 */
@Service
public class AgentOrchestrator {

	private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

	private final AgentRegistry registry;

	public AgentOrchestrator(AgentRegistry registry) {
		this.registry = registry;
	}

	public AgentResponse dispatch(String agentId, AgentRequest request) {
		Agent agent = registry.get(agentId);
		long started = System.nanoTime();
		AgentResponse response = agent.handle(request);
		long elapsedMs = (System.nanoTime() - started) / 1_000_000;
		log.info("agent={} conversationId={} elapsedMs={}", agentId, request.conversationId(), elapsedMs);
		return response;
	}
}
