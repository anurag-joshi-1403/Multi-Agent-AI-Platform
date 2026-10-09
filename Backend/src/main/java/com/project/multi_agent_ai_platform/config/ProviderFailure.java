package com.project.multi_agent_ai_platform.config;

import java.util.Locale;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIServiceException;

/**
 * Why one provider's call failed, in words the console can show. Every provider SDK (Google GenAI,
 * OpenAI - also used for Groq and OpenRouter - and Anthropic) has its own exception family; this
 * reads all of them, so the rest of the app never has to.
 *
 * @param provider   who failed
 * @param kind       what kind of failure it was
 * @param httpStatus the provider's HTTP status, or {@code 0} when it never answered
 * @param detail     the first line of the provider's message
 */
public record ProviderFailure(LlmProvider provider, Kind kind, int httpStatus, String detail) {

	public enum Kind {
		/** The provider rejected the key: 401/403, or Gemini's 400 "API key not valid". */
		BAD_KEY,
		/** Rate limit or a server-side error (429/5xx): it may work again in a moment. */
		BUSY,
		/** No answer at all: DNS, timeout, connection refused. */
		UNREACHABLE,
		/** Any other 4xx, e.g. an unknown model name. */
		REJECTED,
		/** Anything else. */
		FAILED
	}

	/** Spring AI wraps SDK errors (e.g. "Failed to generate content"), so the cause chain is searched. */
	public static ProviderFailure of(LlmProvider provider, Throwable error) {
		Throwable t = error;
		for (int depth = 0; t != null && depth < 10; depth++, t = t.getCause()) {
			if (t instanceof ApiException e) {
				return answered(provider, e.code(), e.message());
			}
			if (t instanceof OpenAIServiceException e) {
				return answered(provider, e.statusCode(), e.getMessage());
			}
			if (t instanceof AnthropicServiceException e) {
				return answered(provider, e.statusCode(), e.getMessage());
			}
			if (t instanceof RestClientResponseException e) {
				return answered(provider, e.getStatusCode().value(), e.getStatusText());
			}
			if (t instanceof GenAiIOException || t instanceof OpenAIIoException || t instanceof AnthropicIoException
					|| t instanceof ResourceAccessException) {
				return new ProviderFailure(provider, Kind.UNREACHABLE, 0, firstLine(t.getMessage()));
			}
		}
		return new ProviderFailure(provider, Kind.FAILED, 0, firstLine(error.getMessage()));
	}

	private static ProviderFailure answered(LlmProvider provider, int status, String message) {
		String lower = message == null ? "" : message.toLowerCase(Locale.ROOT);
		Kind kind;
		if (status == 401 || status == 403 || lower.contains("api key not valid") || lower.contains("api_key_invalid")) {
			kind = Kind.BAD_KEY;
		}
		else if (status == 429 || status >= 500) {
			kind = Kind.BUSY;
		}
		else {
			kind = Kind.REJECTED;
		}
		return new ProviderFailure(provider, kind, status, firstLine(message));
	}

	/** One line for logs, metadata and the "all providers failed" message. */
	public String summary() {
		String name = provider.displayName();
		return switch (kind) {
			case BAD_KEY -> name + " rejected the key (" + provider.keyEnvVar() + ")";
			case BUSY -> name + " is rate-limiting or unavailable (HTTP " + httpStatus + ")";
			case UNREACHABLE -> name + " could not be reached: " + detail;
			case REJECTED -> name + " returned HTTP " + httpStatus + ": " + detail;
			case FAILED -> name + " failed: " + detail;
		};
	}

	/** Worth retrying later, rather than something to fix in the configuration. */
	public boolean temporary() {
		return kind == Kind.BUSY || kind == Kind.UNREACHABLE;
	}

	static String firstLine(String message) {
		if (message == null || message.isBlank()) {
			return "no details from the provider";
		}
		int nl = message.indexOf('\n');
		return (nl < 0 ? message : message.substring(0, nl)).strip();
	}
}
