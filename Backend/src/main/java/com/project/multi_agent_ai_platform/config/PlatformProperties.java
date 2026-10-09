package com.project.multi_agent_ai_platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Platform-level settings, bound from {@code platform.*} in application.properties.
 *
 * @param memory conversation memory settings
 */
@ConfigurationProperties(prefix = "platform")
public record PlatformProperties(@DefaultValue Memory memory) {

	/** @param maxMessages how many recent messages of a conversation are replayed to the model */
	public record Memory(@DefaultValue("20") int maxMessages) {
	}
}
