package com.project.multi_agent_ai_platform.agent.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.project.multi_agent_ai_platform.agent.core.AgentParameter;
import com.project.multi_agent_ai_platform.agent.core.AgentRequest;
import com.project.multi_agent_ai_platform.agent.core.AgentResponse;
import com.project.multi_agent_ai_platform.agent.llm.StubChatModel;

class CodingAgentTest {

	private final StubChatModel model = new StubChatModel();

	private final CodingAgent agent = new CodingAgent(model.clientBuilder(), StubChatModel.memory(),
			StubChatModel.provider());

	@Test
	void describesItselfWithALanguageOption() {
		assertThat(agent.id()).isEqualTo("coding");
		assertThat(agent.name()).isEqualTo("Coding Agent");
		assertThat(agent.capabilities()).containsExactly("Generate", "Explain", "Refactor", "Tests");

		AgentParameter language = agent.parameters().getFirst();
		assertThat(language.name()).isEqualTo("language");
		assertThat(language.type()).isEqualTo(AgentParameter.Type.SELECT);
		assertThat(language.options()).hasSize(22).startsWith("", "TypeScript").contains("Java", "Python");
		assertThat(language.defaultValue()).isEqualTo("");
	}

	@Test
	void chosenLanguageIsPinnedInThePromptAndReported() {
		model.reply = "```java\nclass A {}\n```";

		AgentResponse response = agent.handle(new AgentRequest(null, "write a class", Map.of("language", "Java")));

		assertThat(model.lastPrompt().getUserMessage().getText())
			.startsWith("Answer in Java.")
			.endsWith("write a class");
		assertThat(model.lastPrompt().getSystemMessage().getText()).isEqualTo(CodingAgent.SYSTEM_PROMPT);
		assertThat(response.metadata()).containsEntry("language", "Java");
	}

	@Test
	void autoDetectSendsTheMessageAsIsAndReadsTheLanguageFromTheReply() {
		model.reply = "Here:\n```Python\nprint('hi')\n```";

		AgentResponse response = agent.handle(new AgentRequest(null, "print hi", Map.of("language", "")));

		assertThat(model.lastPrompt().getUserMessage().getText()).isEqualTo("print hi");
		assertThat(response.metadata()).containsEntry("language", "python");
	}

	@Test
	void noCodeBlockMeansNoLanguage() {
		model.reply = "Just prose.";

		assertThat(agent.handle(AgentRequest.of("what is a closure?")).metadata()).doesNotContainKey("language");
	}

	@Test
	void detectLanguageHandlesSymbolsInTags() {
		assertThat(CodingAgent.detectLanguage("```c++\nint x;\n```")).isEqualTo("c++");
		assertThat(CodingAgent.detectLanguage("```C#\nvar x;\n```")).isEqualTo("c#");
		assertThat(CodingAgent.detectLanguage("```\nuntagged\n```")).isNull();
	}
}
