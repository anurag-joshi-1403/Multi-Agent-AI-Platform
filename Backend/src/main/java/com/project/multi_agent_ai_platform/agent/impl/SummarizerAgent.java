package com.project.multi_agent_ai_platform.agent.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import com.project.multi_agent_ai_platform.agent.core.AgentParameter;
import com.project.multi_agent_ai_platform.agent.core.AgentRequest;
import com.project.multi_agent_ai_platform.agent.core.AgentResponse;
import com.project.multi_agent_ai_platform.agent.llm.LlmAgent;
import com.project.multi_agent_ai_platform.document.AttachmentResolver;
import com.project.multi_agent_ai_platform.document.StoredDocument;

/**
 * Condenses text: the message itself, or the attached files when there are any. Attributes:
 * {@code style} = {@code bullets} (default) | {@code tldr} | {@code executive}; {@code maxWords}
 * caps the summary length.
 */
@Component
public class SummarizerAgent extends LlmAgent {

	static final String SYSTEM_PROMPT = """
			You summarise text faithfully. Keep every number, name and decision that matters; drop filler.
			Do not add information that is not in the input. Do not comment on the quality of the input.
			""";

	static final int DEFAULT_MAX_WORDS = 150;

	/** Below this a summary stops being useful, whatever the caller asks for. */
	static final int MIN_WORDS = 20;

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
		int maxWords = Math.max(MIN_WORDS, attribute(request, "maxWords", DEFAULT_MAX_WORDS));

		String instruction = switch (style) {
			case "tldr" -> "Write a single-paragraph TL;DR of at most " + maxWords + " words.";
			case "executive" -> "Write an executive brief: a one-sentence bottom line, then sections titled "
					+ "Why it matters and Next steps. At most " + maxWords + " words in total.";
			default -> "Write 3-7 bullet points, most important first. At most " + maxWords + " words in total.";
		};

		// With files attached, the message is usually "summarise this", so the files are the input and
		// the message only steers the summary. Without files, the message is the text itself.
		List<StoredDocument> attached = attached(request);
		String delimiter = "\"\"\"";
		String prompt;
		int inputChars;
		if (attached.isEmpty()) {
			prompt = instruction + "\n\nText to summarise:\n" + delimiter + "\n" + request.message() + "\n" + delimiter;
			inputChars = request.message().length();
		}
		else {
			prompt = instruction + "\n\nSummarise the attached file(s). The user's request:\n" + delimiter + "\n"
					+ request.message() + "\n" + delimiter;
			inputChars = attached.stream().mapToInt(doc -> doc.content().length()).sum();
		}
		Completion completion = complete(request, prompt, attached);

		Map<String, Object> metadata = new LinkedHashMap<>(completion.metadata());
		metadata.put("style", style);
		metadata.put("maxWords", maxWords);
		metadata.put("inputChars", inputChars);
		if (!completion.content().isEmpty() && inputChars > 0) {
			double ratio = (double) completion.content().length() / inputChars;
			metadata.put("compressionRatio", Math.round(ratio * 100.0) / 100.0);
		}
		return new AgentResponse(id(), completion.content(), metadata);
	}

	/** Unknown or missing styles fall back to bullets. */
	static String normaliseStyle(String style) {
		if (style == null) {
			return "bullets";
		}
		return switch (style.toLowerCase(Locale.ROOT)) {
			case "tldr", "tl;dr" -> "tldr";
			case "executive", "brief" -> "executive";
			default -> "bullets";
		};
	}
}
