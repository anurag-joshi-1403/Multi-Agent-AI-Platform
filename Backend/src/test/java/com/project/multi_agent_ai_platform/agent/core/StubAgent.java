package com.project.multi_agent_ai_platform.agent.core;

/** Minimal {@link Agent} for tests: echoes the message back under a fixed id. */
record StubAgent(String id) implements Agent {

	@Override
	public String description() {
		return "stub " + id;
	}

	@Override
	public AgentResponse handle(AgentRequest request) {
		return AgentResponse.of(id, "echo:" + request.message());
	}
}
