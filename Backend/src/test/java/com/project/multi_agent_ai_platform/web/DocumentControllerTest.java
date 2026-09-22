package com.project.multi_agent_ai_platform.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;
import com.project.multi_agent_ai_platform.config.PlatformProperties;
import com.project.multi_agent_ai_platform.config.SecurityConfig;
import com.project.multi_agent_ai_platform.document.InMemoryDocumentStore;
import com.project.multi_agent_ai_platform.document.DocumentTextExtractor;
import com.project.multi_agent_ai_platform.document.TestPdf;

@WebMvcTest(DocumentController.class)
@Import({ SecurityConfig.class, InMemoryDocumentStore.class, DocumentTextExtractor.class, AgentControllerTest.ProviderConfig.class })
@EnableConfigurationProperties(PlatformProperties.class)
class DocumentControllerTest {
	// Authenticated by default via AgentControllerTest.ProviderConfig#authenticatedByDefault.

	@Autowired
	MockMvc mvc;

	@Test
	void uploadsTextFileThenListsFetchesAndDeletes() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain",
				"The contract renews automatically.".getBytes(StandardCharsets.UTF_8));

		MvcResult created = mvc.perform(multipart("/api/documents").file(file))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/documents/doc_")))
			.andExpect(jsonPath("$.name").value("notes.txt"))
			.andExpect(jsonPath("$.chars").value(34))
			.andExpect(jsonPath("$.pages").doesNotExist())
			.andExpect(jsonPath("$.preview").value("The contract renews automatically."))
			.andReturn();
		String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mvc.perform(get("/api/documents"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(id));

		mvc.perform(get("/api/documents/{id}/content", id))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
			.andExpect(content().string("The contract renews automatically."));

		mvc.perform(delete("/api/documents/{id}", id)).andExpect(status().isNoContent());

		mvc.perform(get("/api/documents/{id}", id))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.title").value("Document not found"))
			.andExpect(jsonPath("$.documentId").value(id));
	}

	@Test
	void uploadsPdfAndReportsPages() throws Exception {
		byte[] pdf = TestPdf.withPages("Page one", "Page two");
		MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", pdf);

		mvc.perform(multipart("/api/documents").file(file))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.pages").value(2))
			.andExpect(jsonPath("$.preview").value(org.hamcrest.Matchers.containsString("[page 1] Page one")));
	}

	@Test
	void pastesText() throws Exception {
		mvc.perform(post("/api/documents/text")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"snippet\",\"content\":\"hello there\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("snippet"))
			.andExpect(jsonPath("$.mediaType").value("text/plain"));
	}

	@Test
	void blankPastedContentIs400() throws Exception {
		mvc.perform(post("/api/documents/text")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"content\":\"  \"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void unsupportedFileTypeIs415() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[] { 1, 2, 3, 4 });

		mvc.perform(multipart("/api/documents").file(file))
			.andExpect(status().isUnsupportedMediaType())
			.andExpect(jsonPath("$.title").value("Unsupported document"));
	}
}
