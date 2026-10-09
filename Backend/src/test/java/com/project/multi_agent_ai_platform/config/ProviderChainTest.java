package com.project.multi_agent_ai_platform.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import com.google.genai.errors.ApiException;
import com.project.multi_agent_ai_platform.agent.llm.StubChatModel;

class ProviderChainTest {

	private static final LlmProvider GROQ = LlmProvider.of("groq", "Groq", "GROQ_API_KEY", "llama", "groq-key");

	private static final LlmProvider GEMINI = LlmProvider.gemini("gemini-test", "gemini-key");

	private static final LlmProvider OPENAI_NO_KEY = LlmProvider.of("openai", "OpenAI", "OPENAI_API_KEY", "gpt", "");

	private static final Prompt PROMPT = new Prompt(new UserMessage("hi"));

	/** A provider that always fails with {@code error}, and counts its calls. */
	private static final class Failing extends StubChatModel {

		private final RuntimeException error;

		Failing(RuntimeException error) {
			this.error = error;
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			prompts.add(prompt);
			throw error;
		}
	}

	private static ProviderChain.Link link(LlmProvider provider, StubChatModel model) {
		return new ProviderChain.Link(provider, model);
	}

	@Test
	void theFirstProviderWithAKeyAnswersAndIsNamed() {
		StubChatModel groq = new StubChatModel();
		StubChatModel gemini = new StubChatModel();
		ProviderChain chain = new ProviderChain(List.of(link(OPENAI_NO_KEY, null), link(GROQ, groq), link(GEMINI, gemini)));

		ChatResponse response = chain.call(PROMPT);

		assertThat(groq.prompts).hasSize(1);
		assertThat(gemini.prompts).isEmpty();
		assertThat((String) response.getMetadata().get(ProviderChain.PROVIDER)).isEqualTo("Groq");
		assertThat((Object) response.getMetadata().get(ProviderChain.FAILED_OVER)).isNull();
		assertThat(response.getResult().getOutput().getText()).isEqualTo("stub reply");
		assertThat(response.getMetadata().getModel()).isEqualTo("stub-model");
		assertThat(response.getMetadata().getUsage().getTotalTokens()).isEqualTo(15);
	}

	@Test
	void aFailingProviderHandsOverToTheNextAndTheFailureIsRecorded() {
		Failing groq = new Failing(new RuntimeException("upstream", new ApiException(401, "UNAUTHENTICATED", "bad key")));
		StubChatModel gemini = new StubChatModel();
		ProviderChain chain = new ProviderChain(List.of(link(GROQ, groq), link(GEMINI, gemini)));

		ChatResponse response = chain.call(PROMPT);

		assertThat(groq.prompts).hasSize(1);
		assertThat(gemini.prompts).hasSize(1);
		assertThat((String) response.getMetadata().get(ProviderChain.PROVIDER)).isEqualTo("Google Gemini");
		List<String> failedOver = response.getMetadata().get(ProviderChain.FAILED_OVER);
		assertThat(failedOver).containsExactly("Groq rejected the key (GROQ_API_KEY)");
	}

	@Test
	void whenEveryProviderFailsAllFailuresAreReportedInOrder() {
		ProviderChain chain = new ProviderChain(List.of(link(GROQ, new Failing(new IllegalStateException("down"))),
				link(GEMINI, new Failing(new ApiException(429, "RESOURCE_EXHAUSTED", "quota")))));

		assertThatThrownBy(() -> chain.call(PROMPT))
			.isInstanceOfSatisfying(ProviderChainException.class, ex -> assertThat(ex.getFailures())
				.extracting(ProviderFailure::summary)
				.containsExactly("Groq failed: down", "Google Gemini is rate-limiting or unavailable (HTTP 429)"));
	}

	@Test
	void providersWithoutAKeyAreNeverCalled() {
		StubChatModel unkeyed = new StubChatModel();
		ProviderChain chain = new ProviderChain(List.of(link(OPENAI_NO_KEY, unkeyed)));

		assertThatThrownBy(() -> chain.call(PROMPT))
			.isInstanceOfSatisfying(ProviderChainException.class, ex -> assertThat(ex.getFailures()).isEmpty());
		assertThat(unkeyed.prompts).isEmpty();
		assertThat(chain.anyActive()).isFalse();
	}

	@Test
	void eachProviderGetsTheMessagesWithoutAnotherProvidersOptions() {
		StubChatModel gemini = new StubChatModel();
		ProviderChain chain = new ProviderChain(List.of(link(GEMINI, gemini)));

		chain.call(new Prompt(List.of(new UserMessage("hi")), ChatOptions.builder().model("gpt-x").temperature(1.5).build()));

		assertThat(gemini.lastPrompt().getOptions()).isNull();
		assertThat(gemini.lastPrompt().getUserMessage().getText()).isEqualTo("hi");
	}

	@Test
	void theConfiguredModelIsReportedWhenTheProviderNamesNone() {
		StubChatModel silent = new StubChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				return ChatResponse.builder()
					.generations(super.call(prompt).getResults())
					.metadata(ChatResponseMetadata.builder().build())
					.build();
			}
		};

		ChatResponse response = new ProviderChain(List.of(link(GEMINI, silent))).call(PROMPT);

		assertThat(response.getMetadata().getModel()).isEqualTo("gemini-test");
	}

	@Test
	void describesTheChainForTheConsole() {
		ProviderChain chain = new ProviderChain(List.of(link(OPENAI_NO_KEY, null), link(GEMINI, new StubChatModel())));

		assertThat(chain.providers()).extracting(LlmProvider::id).containsExactly("openai", "google-genai");
		assertThat(chain.primary()).isEqualTo(GEMINI);
		assertThat(chain.anyActive()).isTrue();
		// with no key anywhere, the first listed provider is still the one to name
		assertThat(new ProviderChain(List.of(link(OPENAI_NO_KEY, null))).primary()).isEqualTo(OPENAI_NO_KEY);
	}
}
