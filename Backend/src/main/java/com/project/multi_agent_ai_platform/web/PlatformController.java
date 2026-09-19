package com.project.multi_agent_ai_platform.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.multi_agent_ai_platform.agent.core.AgentRegistry;
import com.project.multi_agent_ai_platform.config.LlmProviderInfo;
import com.project.multi_agent_ai_platform.config.PlatformProperties;
import com.project.multi_agent_ai_platform.document.DocumentStore;
import com.project.multi_agent_ai_platform.web.dto.PlatformStatus;

/** {@code GET /api/platform} - which provider/model is active and whether it is ready to be called. */
@RestController
public class PlatformController {

	private final LlmProviderInfo providerInfo;

	private final PlatformProperties properties;

	private final AgentRegistry registry;

	private final DocumentStore documents;

	public PlatformController(LlmProviderInfo providerInfo, PlatformProperties properties, AgentRegistry registry,
			DocumentStore documents) {
		this.providerInfo = providerInfo;
		this.properties = properties;
		this.registry = registry;
		this.documents = documents;
	}

	@GetMapping(path = "/api/platform", produces = MediaType.APPLICATION_JSON_VALUE)
	public PlatformStatus status() {
		return new PlatformStatus(
				providerInfo.provider().id(),
				providerInfo.provider().displayName(),
				providerInfo.model(),
				providerInfo.apiKeyConfigured(),
				providerInfo.provider().keyEnvVar(),
				registry.all().size(),
				properties.memory().maxMessages(),
				new PlatformStatus.Documents(documents.size(), properties.documents().maxStored(),
						properties.documents().maxContextChars()));
	}
}
