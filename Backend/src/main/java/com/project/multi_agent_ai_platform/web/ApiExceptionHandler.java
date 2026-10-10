package com.project.multi_agent_ai_platform.web;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.project.multi_agent_ai_platform.agent.core.InvalidAgentRequestException;
import com.project.multi_agent_ai_platform.agent.core.UnknownAgentException;
import com.project.multi_agent_ai_platform.config.ProviderChainException;
import com.project.multi_agent_ai_platform.config.ProviderFailure;
import com.project.multi_agent_ai_platform.document.UnsupportedDocumentException;
import com.project.multi_agent_ai_platform.user.UsernameTakenException;

/**
 * Maps exceptions to RFC 9457 problem details. Extending
 * {@link ResponseEntityExceptionHandler}
 * keeps Spring's own mappings (validation → 400, unknown route → 404, ...) and
 * adds ours on top.
 * <p>
 * Every error leaves as problem+json with a {@code detail} the console shows
 * as-is. A server error
 * without one would look to the console like "backend offline".
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

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
	 * Wrong username or password on {@code POST /api/auth/login}. The same message
	 * either way, so
	 * it cannot be used to find out which usernames exist. (A request with no
	 * session never reaches
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

	/**
	 * No AI provider answered. With one provider tried, the message is about that
	 * provider (with
	 * the key to set, when that is the problem); with several, it lists what went
	 * wrong with each.
	 * 503 when every failure may pass by itself (rate limits, outages), 502 when
	 * something needs
	 * fixing, like a key.
	 */
	@ExceptionHandler(ProviderChainException.class)
	ProblemDetail providersFailed(ProviderChainException ex) {
		List<ProviderFailure> failures = ex.getFailures();
		if (failures.isEmpty()) {
			return problem(HttpStatus.SERVICE_UNAVAILABLE, "No AI provider configured",
					"No AI provider has a key. Set one in Backend/.env (e.g. GEMINI_API_KEY) and restart the backend.");
		}
		if (failures.size() == 1) {
			return oneProvider(failures.getFirst());
		}
		boolean allTemporary = failures.stream().allMatch(ProviderFailure::temporary);
		ProblemDetail problem = problem(allTemporary ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY,
				"All AI providers failed", "Every provider with a key failed: "
						+ failures.stream().map(ProviderFailure::summary).collect(Collectors.joining("; ")) + ".");
		problem.setProperty("failures", failures.stream().map(ProviderFailure::summary).toList());
		return problem;
	}

	/**
	 * Anything not mapped above. A provider failure wrapped by another layer is
	 * still found.
	 */
	@ExceptionHandler(Exception.class)
	ProblemDetail unexpected(Exception ex) {
		for (Throwable cause = ex.getCause(); cause != null && cause != cause.getCause(); cause = cause.getCause()) {
			if (cause instanceof ProviderChainException chain) {
				return providersFailed(chain);
			}
		}
		log.error("Unhandled exception", ex);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error",
				"Something went wrong on the server. Check the backend logs.");
	}

	private static ProblemDetail oneProvider(ProviderFailure f) {
		String name = f.provider().displayName();
		return switch (f.kind()) {
			case BAD_KEY -> problem(HttpStatus.BAD_GATEWAY, "LLM provider rejected the credentials",
					"Authentication with " + name + " failed. Set " + f.provider().keyEnvVar()
							+ " in Backend/.env and restart the backend.");
			case BUSY -> problem(HttpStatus.SERVICE_UNAVAILABLE, "LLM provider temporarily unavailable",
					name + " is rate-limiting or unavailable (HTTP " + f.httpStatus() + "). Retry in a moment.");
			case UNREACHABLE -> problem(HttpStatus.BAD_GATEWAY, "LLM provider unreachable",
					"Could not reach " + name + ": " + f.detail());
			case REJECTED -> problem(HttpStatus.BAD_GATEWAY, "LLM provider rejected the request",
					name + " returned HTTP " + f.httpStatus() + ": " + f.detail());
			case FAILED -> problem(HttpStatus.BAD_GATEWAY, "LLM provider call failed", name + ": " + f.detail());
		};
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setType(URI.create("https://multi-agent-ai-platform/problems/" + status.value()));
		return problem;
	}

	/**
	 * A request body that breaks its validation rules. Spring's own answer only says "Invalid request
	 * content."; this names each field and rule, e.g. {@code password: at least 8 characters}.
	 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		String detail = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> error.getField() + ": " + error.getDefaultMessage())
			.sorted()
			.collect(Collectors.joining("; "));
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Invalid request",
				detail.isEmpty() ? "The request is not valid." : detail);
		return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
	}

	/** Sign-up with a username that is already registered. */
	@ExceptionHandler(UsernameTakenException.class)
	ProblemDetail usernameTaken(UsernameTakenException ex) {
		return problem(HttpStatus.CONFLICT, "Username taken", "That username is already registered. Pick another one.");
	}
}
