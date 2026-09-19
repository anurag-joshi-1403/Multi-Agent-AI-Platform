package com.project.multi_agent_ai_platform.agent.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import com.project.multi_agent_ai_platform.agent.core.AgentParameter;
import com.project.multi_agent_ai_platform.agent.core.AgentRequest;
import com.project.multi_agent_ai_platform.agent.core.AgentResponse;
import com.project.multi_agent_ai_platform.agent.llm.LlmAgent;

/** Writes, explains and refactors code. Honours an optional {@code language} attribute. */
@Component
public class CodingAgent extends LlmAgent {

	static final String SYSTEM_PROMPT = """
			You are a senior software engineer.
			- Answer with working, idiomatic code first, in a fenced code block tagged with the language.
			- Follow with a short explanation (3-6 lines) of the key decisions and any caveats.
			- Prefer the language the user asks for; if none is given, infer it from context or use TypeScript.
			- Never invent APIs. If something is uncertain, say so.
			""";

	private static final Pattern FENCE = Pattern.compile("```([A-Za-z0-9+#._-]+)");

	public CodingAgent(ChatClient.Builder builder, ChatMemory chatMemory) {
		super(builder, chatMemory, SYSTEM_PROMPT);
	}

	@Override
	public String id() {
		return "coding";
	}

	@Override
	public String description() {
		return "Generates, explains and refactors code in mainstream languages with a short rationale.";
	}

	@Override
	public List<String> capabilities() {
		return List.of("Generate", "Explain", "Refactor", "Tests");
	}

	/** Common languages users ask for; first entry is blank ("Auto-detect" in the UI). */
	static final List<String> LANGUAGES = List.of("", "TypeScript", "JavaScript", "Python", "Java", "C#", "C++",
			"C", "Go", "Rust", "Kotlin", "Swift", "PHP", "Ruby", "SQL", "Bash", "HTML", "CSS", "R", "Scala", "Dart",
			"Elixir");

	@Override
	public List<AgentParameter> parameters() {
		return List.of(AgentParameter.select("language", "Language",
				"Target programming language. Leave on Auto-detect to infer it from context.", LANGUAGES, ""));
	}

	@Override
	public AgentResponse handle(AgentRequest request) {
		String language = attribute(request, "language");
		String message = language == null
				? request.message()
				: "Target language: " + language + "\n\n" + request.message();

		Completion completion = complete(request, message);

		Map<String, Object> metadata = new LinkedHashMap<>(completion.metadata());
		String detected = language != null ? language : detectLanguage(completion.content());
		if (detected != null) {
			metadata.put("language", detected);
		}
		return new AgentResponse(id(), completion.content(), metadata);
	}

	/** Language tag of the first fenced block, e.g. a block opened with three backticks and "java". */
	static String detectLanguage(String content) {
		Matcher m = FENCE.matcher(content);
		return m.find() ? m.group(1).toLowerCase() : null;
	}
}
