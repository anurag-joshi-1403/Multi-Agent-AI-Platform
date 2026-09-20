package com.project.multi_agent_ai_platform.agent.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import com.project.multi_agent_ai_platform.agent.core.AgentParameter;
import com.project.multi_agent_ai_platform.agent.core.AgentRequest;
import com.project.multi_agent_ai_platform.agent.core.AgentResponse;
import com.project.multi_agent_ai_platform.agent.llm.LlmAgent;
import com.project.multi_agent_ai_platform.document.AttachmentResolver;

/**
 * Condenses text. Attributes: {@code style} = {@code bullets} (default) | {@code tldr} |
 * {@code executive}; {@code maxWords} caps the summary length.
 */
@Component
public class SummarizerAgent extends LlmAgent {

	static final String SYSTEM_PROMPT = """
			You summarise text faithfully. Keep every number, name and decision that matters; drop filler.
			Do not add information that is not in the input. Do not comment on the quality of the input.
			""";

	private static final int DEFAULT_MAX_WORDS = 150;

	public SummarizerAgent(ChatClient.Builder builder, ChatMemory chatMemory, AttachmentResolver attachments) {
		super(builder, chatMemory, attachments, SYSTEM_PROMPT);
	}

	@Override
	public String id() {
		return "summarizer";
	}

	@Override
	public String description() {
		return "Condenses long text into key points, a TL;DR or an executive brief.";
	}

	@Override
	public List<String> capabilities() {
		return List.of("Bullets", "TL;DR", "Executive brief");
	}

	@Override
	public List<AgentParameter> parameters() {
		return List.of(
				AgentParameter.select("style", "Style", "Shape of the summary.",
						List.of("bullets", "tldr", "executive"), "bullets"),
				AgentParameter.number("maxWords", "Max words", "Upper bound on the summary length.",
						DEFAULT_MAX_WORDS));
	}

	@Override
	public AgentResponse handle(AgentRequest request) {
		String style = normaliseStyle(attribute(request, "style"));
		int maxWords = Math.max(20, attribute(request, "maxWords", DEFAULT_MAX_WORDS));

		String instruction = switch (style) {
			case "tldr" -> "Write a single-paragraph TL;DR of at most " + maxWords + " words.";
			case "executive" -> "Write an executive brief: a one-sentence bottom line, then sections titled "
					+ "Why it matters and Next steps. At most " + maxWords + " words in total.";
			default -> "Write 3-7 bullet points, most important first. At most " + maxWords + " words in total.";
		};

		String delimiter = "\"\"\"";
		Completion completion = complete(request,
				instruction + "\n\nText to summarise:\n" + delimiter + "\n" + request.message() + "\n" + delimiter);

		Map<String, Object> metadata = new LinkedHashMap<>(completion.metadata());
		metadata.put("style", style);
		metadata.put("maxWords", maxWords);
		metadata.put("inputChars", request.message().length());
		if (!completion.content().isEmpty()) {
			double ratio = (double) completion.content().length() / request.message().length();
			metadata.put("compressionRatio", Math.round(ratio * 100.0) / 100.0);
		}
		return new AgentResponse(id(), completion.content(), metadata);
	}

	static String normaliseStyle(String style) {
		if (style == null) {
			return "bullets";
		}
		return switch (style.toLowerCase()) {
			case "tldr", "tl;dr" -> "tldr";
			case "executive", "brief" -> "executive";
			default -> "bullets";
		};
	}
}
