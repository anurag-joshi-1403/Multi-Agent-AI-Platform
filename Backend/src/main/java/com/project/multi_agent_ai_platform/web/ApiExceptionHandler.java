package com.project.multi_agent_ai_platform.web;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIServiceException;
import com.project.multi_agent_ai_platform.agent.core.InvalidAgentRequestException;
import com.project.multi_agent_ai_platform.agent.core.UnknownAgentException;
import com.project.multi_agent_ai_platform.config.LlmProvider;
import com.project.multi_agent_ai_platform.document.DocumentNotFoundException;
import com.project.multi_agent_ai_platform.document.UnsupportedDocumentException;

/**
 * Maps platform exceptions to RFC 9457 problem details. Extending
 * {@link ResponseEntityExceptionHandler} keeps Spring's own mappings (validation → 400, unknown
 * media type → 415, ...) and adds ours on top.
 * <p>
 * Each provider SDK (Google GenAI, Anthropic, OpenAI) has its own exception family; all three are
 * translated through {@link #providerStatus} so the UI can tell "fix your key" from "try again".
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	private final LlmProvider provider;

	public ApiExceptionHandler(LlmProvider provider) {
		this.provider = provider;
	}

	@ExceptionHandler(UnknownAgentException.class)
	ProblemDetail unknownAgent(UnknownAgentException ex) {
		ProblemDetail problem = problem(HttpStatus.NOT_FOUND, "Unknown agent", ex.getMessage());
		problem.setProperty("agentId", ex.getAgentId());
		return problem;
	}

	@ExceptionHandler(DocumentNotFoundException.class)
	ProblemDetail documentNotFound(DocumentNotFoundException ex) {
		ProblemDetail problem = problem(HttpStatus.NOT_FOUND, "Document not found", ex.getMessage());
		problem.setProperty("documentId", ex.getDocumentId());
		return problem;
	}

	@ExceptionHandler({ InvalidAgentRequestException.class, IllegalArgumentException.class })
	ProblemDetail badRequest(RuntimeException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage());
	}

	@ExceptionHandler(UnsupportedDocumentException.class)
	ProblemDetail unsupportedDocument(UnsupportedDocumentException ex) {
		return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported document", ex.getMessage());
	}

	// --- provider SDK errors (non-2xx answers, after the SDK's own retries) -------------------

	@ExceptionHandler(ApiException.class)
	ProblemDetail geminiError(ApiException ex) {
		return providerStatus(ex.code(), ex.message());
	}

	@ExceptionHandler(AnthropicServiceException.class)
	ProblemDetail anthropicError(AnthropicServiceException ex) {
		return providerStatus(ex.statusCode(), ex.getMessage());
	}

	@ExceptionHandler(OpenAIServiceException.class)
	ProblemDetail openAiError(OpenAIServiceException ex) {
		return providerStatus(ex.statusCode(), ex.getMessage());
	}

	@ExceptionHandler(RestClientResponseException.class)
	ProblemDetail restClientError(RestClientResponseException ex) {
		return providerStatus(ex.getStatusCode().value(), ex.getStatusText());
	}

	// --- provider network failures (DNS, timeout, connection refused) -------------------------

	@ExceptionHandler({ GenAiIOException.class, AnthropicIoException.class, OpenAIIoException.class,
			ResourceAccessException.class })
	ProblemDetail providerUnreachable(RuntimeException ex) {
		log.warn("{} unreachable: {}", provider.displayName(), ex.getMessage());
		return problem(HttpStatus.BAD_GATEWAY, "LLM provider unreachable",
				"Could not reach " + provider.displayName() + ": " + firstLine(ex.getMessage()));
	}

	// --- anything else from a provider SDK (malformed data, SSE errors) is still theirs, not ours

	@ExceptionHandler({ AnthropicException.class, OpenAIException.class })
	ProblemDetail providerFailure(RuntimeException ex) {
		log.warn("{} call failed: {}", provider.displayName(), ex.getMessage());
		return problem(HttpStatus.BAD_GATEWAY, "LLM provider call failed", firstLine(ex.getMessage()));
	}

	/**
	 * Spring AI's Gemini model wraps SDK failures in a plain {@code RuntimeException("Failed to
	 * generate content")}, so walk the cause chain before giving up and calling it a 500.
	 */
	@ExceptionHandler(Exception.class)
	ProblemDetail unexpected(Exception ex) {
		for (Throwable cause = ex.getCause(); cause != null && cause != cause.getCause(); cause = cause.getCause()) {
			if (cause instanceof ApiException api) {
				return geminiError(api);
			}
			if (cause instanceof AnthropicServiceException anthropic) {
				return anthropicError(anthropic);
			}
			if (cause instanceof OpenAIServiceException openAi) {
				return openAiError(openAi);
			}
			if (cause instanceof GenAiIOException || cause instanceof AnthropicIoException
					|| cause instanceof OpenAIIoException || cause instanceof ResourceAccessException) {
				return providerUnreachable((RuntimeException) cause);
			}
		}
		log.error("Unhandled exception", ex);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error",
				"Something went wrong on the server. Check the backend logs.");
	}

	/**
	 * 401/403 → 502 with a "set the key" hint (Gemini reports a bad key as 400 INVALID_ARGUMENT
	 * "API key not valid", so that text is treated the same way); 429/5xx → 503 retry-later;
	 * anything else → 502 with the provider's first line.
	 */
	private ProblemDetail providerStatus(int status, String message) {
		String text = message == null ? "" : message;
		String lower = text.toLowerCase();
		log.warn("{} answered {}: {}", provider.displayName(), status, firstLine(text));
		boolean badKey = status == 401 || status == 403 || lower.contains("api key not valid")
				|| lower.contains("api_key_invalid");
		if (badKey) {
			return problem(HttpStatus.BAD_GATEWAY, "LLM provider rejected the credentials",
					"Authentication with " + provider.displayName() + " failed. Set " + provider.keyEnvVar()
							+ " and restart the backend.");
		}
		if (status == 429 || status >= 500) {
			return problem(HttpStatus.SERVICE_UNAVAILABLE, "LLM provider temporarily unavailable",
					provider.displayName() + " is rate-limiting or unavailable (HTTP " + status + "). Retry in a moment.");
		}
		return problem(HttpStatus.BAD_GATEWAY, "LLM provider rejected the request",
				provider.displayName() + " returned HTTP " + status + ": " + firstLine(text));
	}

	private static String firstLine(String message) {
		if (message == null || message.isBlank()) {
			return "no details from the provider";
		}
		int nl = message.indexOf('\n');
		return (nl < 0 ? message : message.substring(0, nl)).strip();
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setType(URI.create("https://multi-agent-ai-platform/problems/" + status.value()));
		return problem;
	}
}
