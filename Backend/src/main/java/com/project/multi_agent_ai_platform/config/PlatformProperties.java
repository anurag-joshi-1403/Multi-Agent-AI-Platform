package com.project.multi_agent_ai_platform.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Platform-level knobs, bound from {@code platform.*} in application.properties.
 *
 * @param cors      browser origin patterns allowed to call the API directly (the Vite proxy needs none)
 * @param memory    conversation memory settings
 * @param documents document upload / context settings
 * @param auth      console login accounts
 */
@ConfigurationProperties(prefix = "platform")
public record PlatformProperties(
		@DefaultValue Cors cors,
		@DefaultValue Memory memory,
		@DefaultValue Documents documents,
		@DefaultValue Auth auth) {

	/**
	 * Vite falls back to the next free port (5174, 5175, ...) whenever 5173 is taken, so the
	 * default matches any localhost port rather than pinning one that silently goes stale.
	 */
	public record Cors(
			@DefaultValue({ "http://localhost:*", "http://127.0.0.1:*" }) List<String> allowedOriginPatterns) {
	}

	/** @param maxMessages how many recent messages of a conversation are replayed to the model */
	public record Memory(@DefaultValue("20") int maxMessages) {
	}

	/**
	 * @param maxContextChars document text beyond this is truncated before it reaches the model
	 * @param maxStored       oldest documents are evicted once this many are held in memory
	 */
	public record Documents(@DefaultValue("60000") int maxContextChars, @DefaultValue("50") int maxStored) {
	}

	/**
	 * Console accounts, as {@code username:password} pairs separated by commas — e.g.
	 * {@code "anurag:s3cret,guest:letmein"}. The default {@code admin:admin} is a placeholder for
	 * local development only; set {@code AUTH_USERS} before running this anywhere but your own
	 * machine.
	 *
	 * @param users raw {@code username:password,...} list; parsed by {@code SecurityConfig}
	 */
	public record Auth(@DefaultValue("admin:admin") String users) {
	}
}
