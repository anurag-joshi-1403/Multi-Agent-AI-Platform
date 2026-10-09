package com.project.multi_agent_ai_platform.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.project.multi_agent_ai_platform.agent.llm.StubChatModel;
import com.project.multi_agent_ai_platform.config.LlmProvider;

@WebMvcTest(ConversationController.class)
@Import({ ConversationControllerTest.Config.class, SignedInWebTest.class })
class ConversationControllerTest {

	@TestConfiguration
	static class Config {

		@Bean
		LlmProvider llmProvider() {
			return StubChatModel.provider();
		}

		@Bean
		ChatMemory chatMemory() {
			return StubChatModel.memory();
		}
	}

	@Autowired
	MockMvc mvc;

	@Autowired
	ChatMemory memory;

	@Test
	void forgetsOneConversationAndLeavesOthersAlone() throws Exception {
		memory.add("c-1", List.of(new UserMessage("hi"), new AssistantMessage("hello")));
		memory.add("c-2", List.of(new UserMessage("other")));

		mvc.perform(delete("/api/conversations/c-1"))
			.andExpect(status().isNoContent())
			.andExpect(content().string(""));

		assertThat(memory.get("c-1")).isEmpty();
		assertThat(memory.get("c-2")).hasSize(1);
	}

	@Test
	void forgettingAnUnknownConversationIsStill204() throws Exception {
		mvc.perform(delete("/api/conversations/never-existed")).andExpect(status().isNoContent());
	}
}
