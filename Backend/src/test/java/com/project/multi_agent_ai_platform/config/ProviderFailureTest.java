package com.project.multi_agent_ai_platform.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.anthropic.errors.AnthropicIoException;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.openai.core.http.Headers;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.RateLimitException;
import com.openai.errors.UnauthorizedException;

class ProviderFailureTest {

	private static final LlmProvider GEMINI = LlmProvider.gemini("gemini-test", "k");

	private static final LlmProvider OPENAI = LlmProvider.of("openai", "OpenAI", "OPENAI_API_KEY", "gpt", "k");

	@Test
	void geminiErrorsAreReadFromTheirStatus() {
		assertThat(ProviderFailure.of(GEMINI, new ApiException(401, "UNAUTHENTICATED", "no")).kind())
			.isEqualTo(ProviderFailure.Kind.BAD_KEY);
		// Gemini reports a bad key as 400 INVALID_ARGUMENT
		assertThat(ProviderFailure.of(GEMINI, new ApiException(400, "INVALID_ARGUMENT", "API key not valid.")).kind())
			.isEqualTo(ProviderFailure.Kind.BAD_KEY);
		assertThat(ProviderFailure.of(GEMINI, new ApiException(429, "RESOURCE_EXHAUSTED", "quota")).kind())
			.isEqualTo(ProviderFailure.Kind.BUSY);
		assertThat(ProviderFailure.of(GEMINI, new ApiException(503, "UNAVAILABLE", "overloaded")).kind())
			.isEqualTo(ProviderFailure.Kind.BUSY);

		ProviderFailure notFound = ProviderFailure.of(GEMINI, new ApiException(404, "NOT_FOUND", "no model\nmore"));
		assertThat(notFound.kind()).isEqualTo(ProviderFailure.Kind.REJECTED);
		assertThat(notFound.httpStatus()).isEqualTo(404);
		assertThat(notFound.summary()).isEqualTo("Google Gemini returned HTTP 404: no model");
	}

	@Test
	void openAiSdkErrorsAreReadToo() {
		Headers none = Headers.builder().build();

		assertThat(ProviderFailure.of(OPENAI, UnauthorizedException.builder().headers(none).build()).summary())
			.isEqualTo("OpenAI rejected the key (OPENAI_API_KEY)");
		assertThat(ProviderFailure.of(OPENAI, RateLimitException.builder().headers(none).build()).kind())
			.isEqualTo(ProviderFailure.Kind.BUSY);
	}

	@Test
	void noAnswerAtAllIsUnreachable() {
		assertThat(ProviderFailure.of(GEMINI, new GenAiIOException("connect timed out")).kind())
			.isEqualTo(ProviderFailure.Kind.UNREACHABLE);
		assertThat(ProviderFailure.of(OPENAI, new OpenAIIoException("reset")).summary())
			.isEqualTo("OpenAI could not be reached: reset");
		assertThat(ProviderFailure.of(OPENAI, new AnthropicIoException("dns")).temporary()).isTrue();
	}

	@Test
	void theCauseChainIsSearchedForTheProvidersOwnError() {
		RuntimeException wrapped = new RuntimeException("Failed to generate content",
				new IllegalStateException("retry exhausted", new ApiException(429, "RESOURCE_EXHAUSTED", "quota")));

		assertThat(ProviderFailure.of(GEMINI, wrapped).kind()).isEqualTo(ProviderFailure.Kind.BUSY);
	}

	@Test
	void anythingElseIsAPlainFailureWithItsFirstLine() {
		ProviderFailure failure = ProviderFailure.of(GEMINI, new IllegalStateException("boom\nstack"));

		assertThat(failure.kind()).isEqualTo(ProviderFailure.Kind.FAILED);
		assertThat(failure.summary()).isEqualTo("Google Gemini failed: boom");
		assertThat(failure.temporary()).isFalse();
		assertThat(ProviderFailure.of(GEMINI, new IllegalStateException()).detail()).isEqualTo("no details from the provider");
	}
}
