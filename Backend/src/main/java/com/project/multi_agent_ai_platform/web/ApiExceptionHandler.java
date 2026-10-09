package com.project.multi_agent_ai_platform.web;

import java.net.URI;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.project.multi_agent_ai_platform.agent.core.InvalidAgentRequestException;
import com.project.multi_agent_ai_platform.agent.core.UnknownAgentException;
import com.project.multi_agent_ai_platform.config.LlmProvider;
import com.project.multi_agent_ai_platform.document.UnsupportedDocumentException;

/**
 * Maps exceptions to RFC 9457 problem details. Extending {@link ResponseEntityExceptionHandler}
 * keeps Spring's own mappings (validation → 400, unknown route → 404, ...) and adds ours on top.
 * <p>
 * Every error leaves as problem+json with a {@code detail} the console shows as-is. A server error
 * without one would look to the console like "backend offline".
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

	@ExceptionHandler({ InvalidAgentRequestException.class, IllegalArgumentException.class })
	ProblemDetail badRequest(RuntimeException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage());
	}

	/**
	 * Wrong username or password on {@code POST /api/auth/login}. The same message either way, so
	 * it cannot be used to find out which usernames exist. (A request with no session never reaches
	 * a controller; SecurityConfig's entry point answers that one.)
	 */
	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail authenticationFailed(AuthenticationException ex) {
		return problem(HttpStatus.UNAUTHORIZED, "Invalid credentials", "Incorrect username or password.");
	}

	@ExceptionHandler(UnsupportedDocumentException.class)
	ProblemDetail unsupportedDocument(UnsupportedDocumentException ex) {
		return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported document", ex.getMessage());
	}

	/** Gemini answered with a non-2xx status. */
	@ExceptionHandler(ApiException.class)
	ProblemDetail providerError(ApiException ex) {
		return providerStatus(ex.code(), ex.message());
	}

	/** Gemini could not be reached at all (DNS, timeout, connection refused). */
	@ExceptionHandler({ GenAiIOException.class, ResourceAccessException.class })
	ProblemDetail providerUnreachable(RuntimeException ex) {
		log.warn("{} unreachable: {}", provider.displayName(), ex.getMessage());
		return problem(HttpStatus.BAD_GATEWAY, "LLM provider unreachable",
				"Could not reach " + provider.displayName() + ": " + firstLine(ex.getMessage()));
	}

	/**
	 * Spring AI wraps Gemini SDK failures in a plain {@code RuntimeException("Failed to generate
	 * content")}, so walk the cause chain before giving up and calling it a 500.
	 */
	@ExceptionHandler(Exception.class)
	ProblemDetail unexpected(Exception ex) {
		for (Throwable cause = ex.getCause(); cause != null && cause != cause.getCause(); cause = cause.getCause()) {
			if (cause instanceof ApiException api) {
				return providerError(api);
			}
			if (cause instanceof GenAiIOException || cause instanceof ResourceAccessException) {
				return providerUnreachable((RuntimeException) cause);
			}
		}
		log.error("Unhandled exception", ex);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error",
				"Something went wrong on the server. Check the backend logs.");
	}

	/**
	 * 401/403 → 502 with a "set the key" hint (Gemini reports a bad key as 400 INVALID_ARGUMENT
	 * "API key not valid", so that text counts too); 429/5xx → 503 retry later; anything else → 502
	 * with the provider's first line.
	 */
	private ProblemDetail providerStatus(int status, String message) {
		String text = message == null ? "" : message;
		String lower = text.toLowerCase(Locale.ROOT);
		log.warn("{} answered {}: {}", provider.displayName(), status, firstLine(text));
		boolean badKey = status == 401 || status == 403 || lower.contains("api key not valid")
				|| lower.contains("api_key_invalid");
		if (badKey) {
			return problem(HttpStatus.BAD_GATEWAY, "LLM provider rejected the credentials",
					"Authentication with " + provider.displayName() + " failed. Set " + provider.keyEnvVar()
							+ " in Backend/.env and restart the backend.");
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
