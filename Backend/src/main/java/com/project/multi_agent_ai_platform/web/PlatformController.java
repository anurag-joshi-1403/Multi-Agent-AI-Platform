package com.project.multi_agent_ai_platform.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.multi_agent_ai_platform.agent.core.AgentRegistry;
import com.project.multi_agent_ai_platform.config.LlmProvider;
import com.project.multi_agent_ai_platform.config.PlatformProperties;
import com.project.multi_agent_ai_platform.document.DocumentStore;
import com.project.multi_agent_ai_platform.web.dto.PlatformStatus;

/** {@code GET /api/platform} - which provider and model are active, whether a key is set, and the limits. */
@RestController
public class PlatformController {

	private final LlmProvider provider;

	private final PlatformProperties properties;

	private final AgentRegistry registry;

	private final DocumentStore documents;

	public PlatformController(LlmProvider provider, PlatformProperties properties, AgentRegistry registry,
			DocumentStore documents) {
		this.provider = provider;
		this.properties = properties;
		this.registry = registry;
		this.documents = documents;
	}

	@GetMapping(path = "/api/platform", produces = MediaType.APPLICATION_JSON_VALUE)
	public PlatformStatus status() {
		return new PlatformStatus(
				provider.id(),
				provider.displayName(),
				provider.model(),
				provider.apiKeyConfigured(),
				provider.keyEnvVar(),
				registry.all().size(),
				properties.memory().maxMessages(),
				new PlatformStatus.Documents(documents.size(), properties.documents().maxStored(),
						properties.documents().maxContextChars()));
	}
}
