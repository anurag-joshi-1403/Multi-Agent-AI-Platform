package com.project.multi_agent_ai_platform.config;

import java.util.List;
import java.util.stream.Collectors;

/**
 * No provider in the chain produced an answer: each one with a key failed, or none has a key.
 * {@code ApiExceptionHandler} turns this into the problem+json the console shows.
 */
public class ProviderChainException extends RuntimeException {

	private final List<ProviderFailure> failures;

	/** @param failures one per provider tried, in order; empty when no provider has a key */
	public ProviderChainException(List<ProviderFailure> failures) {
		super(failures.isEmpty() ? "No AI provider has a key"
				: failures.stream().map(ProviderFailure::summary).collect(Collectors.joining("; ")));
		this.failures = List.copyOf(failures);
	}

	public List<ProviderFailure> getFailures() {
		return failures;
	}
}
