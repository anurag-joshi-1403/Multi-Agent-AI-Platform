package com.project.multi_agent_ai_platform.agent.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import com.project.multi_agent_ai_platform.agent.core.AgentRequest;
import com.project.multi_agent_ai_platform.agent.core.AgentResponse;
import com.project.multi_agent_ai_platform.agent.llm.LlmAgent;
import com.project.multi_agent_ai_platform.document.AttachmentResolver;

/**
 * Structured research from model knowledge. Live web search arrives in Phase 7; until then the
 * agent says so and flags uncertainty instead of inventing citations.
 */
@Component
public class ResearchAgent extends LlmAgent {

	static final String SYSTEM_PROMPT = """
			You are a careful research analyst working from your own knowledge (no live web access).
			Structure every answer with these markdown headings:
			**Summary** - two or three sentences.
			**Key findings** - a bulleted list; each bullet is one verifiable claim.
			**Open questions** - what you could not establish or what may have changed recently.
			**Confidence** - one of: high, medium, low - and why.
			Never fabricate sources, URLs, statistics or quotations. If you do not know, say so.
			""";

	private static final Pattern CONFIDENCE = Pattern
		.compile("confidence(?:\\s+level)?[*_:#\\s\\-\\u2013\\u2014]*(high|medium|low)\\b", Pattern.CASE_INSENSITIVE);

	public ResearchAgent(ChatClient.Builder builder, ChatMemory chatMemory, AttachmentResolver attachments) {
		super(builder, chatMemory, attachments, SYSTEM_PROMPT);
	}

	@Override
	public String id() {
		return "research";
	}

	@Override
	public String description() {
		return "Breaks a topic into findings, open questions and a confidence rating. Knowledge-based; live web search arrives later.";
	}

	@Override
	public List<String> capabilities() {
		return List.of("Analysis", "Findings", "Confidence");
	}

	@Override
	public AgentResponse handle(AgentRequest request) {
		Completion completion = complete(request, request.message());

		Map<String, Object> metadata = new LinkedHashMap<>(completion.metadata());
		metadata.put("mode", "knowledge");
		String confidence = extractConfidence(completion.content());
		if (confidence != null) {
			metadata.put("confidence", confidence);
		}
		return new AgentResponse(id(), completion.content(), metadata);
	}

	/**
	 * Pulls high / medium / low out of the Confidence section, if the model followed the format:
	 * the level must come right after the heading ({@code **Confidence** - medium},
	 * {@code Confidence: High}), so "medium, not high" reads as medium. The last match wins, since
	 * the section closes the answer.
	 */
	static String extractConfidence(String content) {
		Matcher m = CONFIDENCE.matcher(content);
		String found = null;
		while (m.find()) {
			found = m.group(1).toLowerCase(Locale.ROOT);
		}
		return found;
	}
}
