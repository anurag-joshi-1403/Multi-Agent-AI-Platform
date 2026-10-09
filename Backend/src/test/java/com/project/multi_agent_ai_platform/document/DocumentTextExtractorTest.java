package com.project.multi_agent_ai_platform.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class DocumentTextExtractorTest {

	private final DocumentTextExtractor extractor = new DocumentTextExtractor();

	private static byte[] utf8(String text) {
		return text.getBytes(StandardCharsets.UTF_8);
	}

	@Test
	void extractsPlainText() {
		DocumentTextExtractor.Extracted result = extractor.extract("notes.md", "text/markdown", utf8("hello\nworld"));

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
	void detectsPdfByMagicBytesWhenTheTypeIsGeneric() throws IOException {
		DocumentTextExtractor.Extracted result = extractor.extract("blob", "application/octet-stream",
				TestPdf.withPages("magic"));

		assertThat(result.pages()).isEqualTo(1);
	}

	@Test
	void acceptsEveryCodeExtensionTheConsoleOffersWhateverTheBrowserCallsIt() {
		// Windows browsers report .ts as an MPEG transport stream
		assertThat(extractor.extract("app.ts", "video/mp2t", utf8("const x = 1")).text()).isEqualTo("const x = 1");
		assertThat(extractor.extract("App.tsx", "", utf8("<App />")).text()).isEqualTo("<App />");
		assertThat(extractor.extract("App.jsx", "application/x-unknown", utf8("<App />")).text()).isEqualTo("<App />");
	}

	@Test
	void rejectsEmptyBlankAndUnsupportedFiles() {
		assertThatThrownBy(() -> extractor.extract("x.txt", "text/plain", new byte[0]))
			.isInstanceOf(UnsupportedDocumentException.class);
		assertThatThrownBy(() -> extractor.extract("x.txt", "text/plain", utf8("  \n ")))
			.isInstanceOf(UnsupportedDocumentException.class)
			.hasMessageContaining("no text");
		assertThatThrownBy(() -> extractor.extract("img.png", "image/png", new byte[] { 1, 2, 3 }))
			.isInstanceOf(UnsupportedDocumentException.class)
			.hasMessageContaining("image/png");
	}

	@Test
	void rejectsBinaryPretendingToBeText() {
		byte[] binary = new byte[200];
		for (int i = 0; i < binary.length; i++) {
			binary[i] = (byte) (0x80 + (i % 64));
		}

		assertThatThrownBy(() -> extractor.extract("data.txt", "text/plain", binary))
			.isInstanceOf(UnsupportedDocumentException.class)
			.hasMessageContaining("UTF-8");
	}

	@Test
	void rejectsCorruptPdf() {
		assertThatThrownBy(() -> extractor.extract("bad.pdf", "application/pdf", utf8("%PDF-1.4 not really a pdf")))
			.isInstanceOf(UnsupportedDocumentException.class);
	}
}
