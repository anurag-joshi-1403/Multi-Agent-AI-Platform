package com.project.multi_agent_ai_platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Platform-level settings, bound from {@code platform.*} in application.properties.
 *
 * @param memory    conversation memory settings
 * @param documents document upload and context settings
 */
@ConfigurationProperties(prefix = "platform")
public record PlatformProperties(@DefaultValue Memory memory, @DefaultValue Documents documents) {

	/** @param maxMessages how many recent messages of a conversation are replayed to the model */
	public record Memory(@DefaultValue("20") int maxMessages) {
	}

	/**
	 * @param maxContextChars attached file text beyond this is cut before it reaches the model
	 * @param maxStored       once this many uploads are stored, saving a new one deletes the oldest
	 */
	public record Documents(@DefaultValue("60000") int maxContextChars, @DefaultValue("50") int maxStored) {
	}
}
