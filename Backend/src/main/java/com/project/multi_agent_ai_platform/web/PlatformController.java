package com.project.multi_agent_ai_platform.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.multi_agent_ai_platform.agent.core.AgentRegistry;
import com.project.multi_agent_ai_platform.config.LlmProvider;
import com.project.multi_agent_ai_platform.config.PlatformProperties;
import com.project.multi_agent_ai_platform.config.ProviderChain;
import com.project.multi_agent_ai_platform.document.DocumentStore;
import com.project.multi_agent_ai_platform.web.dto.PlatformStatus;
import com.project.multi_agent_ai_platform.web.dto.ProviderStatus;

/** {@code GET /api/platform} - the provider failover chain, whether any key is set, and the limits. */
@RestController
public class PlatformController {

	private final ProviderChain chain;

	private final PlatformProperties properties;

	private final AgentRegistry registry;

	private final DocumentStore documents;

	public PlatformController(ProviderChain chain, PlatformProperties properties, AgentRegistry registry,
			DocumentStore documents) {
		this.chain = chain;
		this.properties = properties;
		this.registry = registry;
		this.documents = documents;
	}

	@GetMapping(path = "/api/platform", produces = MediaType.APPLICATION_JSON_VALUE)
	public PlatformStatus status() {
		LlmProvider first = chain.primary();
		return new PlatformStatus(
				first.id(),
				first.displayName(),
				first.model(),
				chain.anyActive(),
				first.keyEnvVar(),
				registry.all().size(),
				properties.memory().maxMessages(),
				new PlatformStatus.Documents(documents.size(), properties.documents().maxStored(),
						properties.documents().maxContextChars()),
				chain.providers().stream().map(ProviderStatus::of).toList());
	}
}
