package com.project.multi_agent_ai_platform.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;
import com.project.multi_agent_ai_platform.agent.llm.StubChatModel;
import com.project.multi_agent_ai_platform.config.LlmProvider;
import com.project.multi_agent_ai_platform.document.DocumentStore;
import com.project.multi_agent_ai_platform.document.DocumentTextExtractor;
import com.project.multi_agent_ai_platform.document.FakeDocumentStore;
import com.project.multi_agent_ai_platform.document.TestPdf;

@WebMvcTest(DocumentController.class)
@Import({ DocumentTextExtractor.class, DocumentControllerTest.Config.class })
class DocumentControllerTest {

	@TestConfiguration
	static class Config {

		@Bean
		LlmProvider llmProvider() {
			return StubChatModel.provider();
		}

		@Bean
		DocumentStore documentStore() {
			return new FakeDocumentStore();
		}
	}

	@Autowired
	MockMvc mvc;

	@Autowired
	DocumentStore store;

	private static MockMultipartFile file(String name, String type, byte[] bytes) {
		return new MockMultipartFile("file", name, type, bytes);
	}

	@Test
	void uploadsATextFileInTheShapeTheConsoleExpectsThenDeletesIt() throws Exception {
		byte[] text = "The contract renews automatically.".getBytes(StandardCharsets.UTF_8);

		MvcResult created = mvc.perform(multipart("/api/documents").file(file("notes.txt", "text/plain", text)))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", matchesPattern("/api/documents/doc_\\w+")))
			.andExpect(jsonPath("$.id").isNotEmpty())
			.andExpect(jsonPath("$.name").value("notes.txt"))
			.andExpect(jsonPath("$.mediaType").value("text/plain"))
			.andExpect(jsonPath("$.chars").value(34))
			.andExpect(jsonPath("$.pages").isEmpty())
			.andExpect(jsonPath("$.uploadedAt").isString())
			.andExpect(jsonPath("$.preview").value("The contract renews automatically."))
			.andReturn();
		String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
		assertThat(store.find(id)).isPresent();

		mvc.perform(delete("/api/documents/{id}", id)).andExpect(status().isNoContent());
		assertThat(store.find(id)).isEmpty();
		// idempotent: the console deletes best-effort, so a repeat must not fail
		mvc.perform(delete("/api/documents/{id}", id)).andExpect(status().isNoContent());
	}

	@Test
	void uploadsAPdfAndReportsItsPages() throws Exception {
		byte[] pdf = TestPdf.withPages("Page one", "Page two");

		mvc.perform(multipart("/api/documents").file(file("doc.pdf", "application/pdf", pdf)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.pages").value(2))
			.andExpect(jsonPath("$.preview").value(containsString("[page 1] Page one")));
	}

	@Test
	void unsupportedFileTypeIs415ProblemDetail() throws Exception {
		mvc.perform(multipart("/api/documents").file(file("photo.png", "image/png", new byte[] { 1, 2, 3, 4 })))
			.andExpect(status().isUnsupportedMediaType())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Unsupported document"))
			.andExpect(jsonPath("$.detail").value(containsString("image/png")));
	}

	@Test
	void emptyFileIs415() throws Exception {
		mvc.perform(multipart("/api/documents").file(file("empty.txt", "text/plain", new byte[0])))
			.andExpect(status().isUnsupportedMediaType())
			.andExpect(jsonPath("$.detail").value("The uploaded file is empty."));
	}

	@Test
	void missingFilePartIs400ProblemDetail() throws Exception {
		MockMultipartFile wrongPart = new MockMultipartFile("upload", "x.txt", "text/plain", new byte[] { 'x' });

		mvc.perform(multipart("/api/documents").file(wrongPart))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}
}
