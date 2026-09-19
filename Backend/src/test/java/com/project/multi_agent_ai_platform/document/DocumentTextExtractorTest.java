package com.project.multi_agent_ai_platform.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class DocumentTextExtractorTest {

	private final DocumentTextExtractor extractor = new DocumentTextExtractor();

	@Test
	void extractsPlainText() {
		byte[] bytes = "hello\nworld".getBytes(StandardCharsets.UTF_8);

		DocumentTextExtractor.Extracted result = extractor.extract("notes.md", "text/markdown", bytes);

		assertThat(result.text()).isEqualTo("hello\nworld");
		assertThat(result.pages()).isNull();
	}

	@Test
	void extractsPdfPageByPage() throws IOException {
		byte[] pdf = TestPdf.withPages("First page text", "Second page text");

		DocumentTextExtractor.Extracted result = extractor.extract("doc.pdf", "application/pdf", pdf);

		assertThat(result.pages()).isEqualTo(2);
		assertThat(result.text())
			.contains("[page 1]")
			.contains("First page text")
			.contains("[page 2]")
			.contains("Second page text");
	}

	@Test
	void detectsPdfByMagicBytesWhenContentTypeIsGeneric() throws IOException {
		byte[] pdf = TestPdf.withPages("magic");

		DocumentTextExtractor.Extracted result = extractor.extract("blob", "application/octet-stream", pdf);

		assertThat(result.pages()).isEqualTo(1);
	}

	@Test
	void rejectsEmptyAndUnsupportedFiles() {
		assertThatThrownBy(() -> extractor.extract("x.txt", "text/plain", new byte[0]))
			.isInstanceOf(UnsupportedDocumentException.class);
		assertThatThrownBy(() -> extractor.extract("img.png", "image/png", new byte[] { 1, 2, 3 }))
			.isInstanceOf(UnsupportedDocumentException.class)
			.hasMessageContaining("image/png");
	}

	@Test
	void rejectsCorruptPdf() {
		byte[] bogus = "%PDF-1.4 this is not really a pdf".getBytes(StandardCharsets.UTF_8);

		assertThatThrownBy(() -> extractor.extract("bad.pdf", "application/pdf", bogus))
			.isInstanceOf(UnsupportedDocumentException.class);
	}
}
