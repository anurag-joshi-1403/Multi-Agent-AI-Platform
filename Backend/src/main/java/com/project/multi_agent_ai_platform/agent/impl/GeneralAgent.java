package com.project.multi_agent_ai_platform.agent.impl;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import com.project.multi_agent_ai_platform.agent.core.AgentRequest;
import com.project.multi_agent_ai_platform.agent.core.AgentResponse;
import com.project.multi_agent_ai_platform.agent.llm.LlmAgent;

/** Fallback conversational agent for anything the specialists do not cover. */
@Component
public class GeneralAgent extends LlmAgent {

	static final String SYSTEM_PROMPT = """
			You are a helpful, direct assistant. Answer plainly in markdown. Ask a clarifying question only
			when the request is genuinely ambiguous. Keep answers as short as the question allows.
			""";

	public GeneralAgent(ChatClient.Builder builder, ChatMemory chatMemory) {
		super(builder, chatMemory, SYSTEM_PROMPT);
	}

	@Override
	public String id() {
		return "general";
	}

	@Override
	public String name() {
		return "General Assistant";
	}

	@Override
	public String description() {
		return "Fallback conversational assistant for anything the specialists do not cover.";
	}

	@Override
	public List<String> capabilities() {
		return List.of("Chat", "Brainstorm", "Drafting");
	}

	@Override
	public AgentResponse handle(AgentRequest request) {
		Completion completion = complete(request, request.message());
		return new AgentResponse(id(), completion.content(), completion.metadata());
	}
}
