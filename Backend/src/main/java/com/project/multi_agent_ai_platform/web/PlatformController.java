package com.project.multi_agent_ai_platform.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.multi_agent_ai_platform.agent.core.AgentRegistry;
import com.project.multi_agent_ai_platform.config.LlmProvider;
import com.project.multi_agent_ai_platform.web.dto.PlatformStatus;

/** {@code GET /api/platform} - which provider and model are active, and whether a key is set. */
@RestController
public class PlatformController {

	private final LlmProvider provider;

	private final AgentRegistry registry;

	public PlatformController(LlmProvider provider, AgentRegistry registry) {
		this.provider = provider;
		this.registry = registry;
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
				0,
				PlatformStatus.Documents.NONE);
	}
}
