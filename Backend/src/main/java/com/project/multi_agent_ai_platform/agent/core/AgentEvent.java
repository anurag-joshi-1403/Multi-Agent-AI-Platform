package com.project.multi_agent_ai_platform.agent.core;

/** One step of a streamed answer: pieces of text as they are written, then the finished answer. */
public sealed interface AgentEvent {

	/** The next piece of the answer's text. */
	record Chunk(String text) implements AgentEvent {
	}

	/** The whole answer, with the same metadata a non-streamed run returns. Always the last event. */
	record Done(AgentResponse response) implements AgentEvent {
	}
}
