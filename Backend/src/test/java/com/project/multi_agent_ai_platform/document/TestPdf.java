package com.project.multi_agent_ai_platform.document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/** Builds tiny real PDFs for tests, one line of text per page. */
public final class TestPdf {

	private TestPdf() {
	}

	public static byte[] withPages(String... pageTexts) throws IOException {
		try (PDDocument doc = new PDDocument()) {
			for (String text : pageTexts) {
				PDPage page = new PDPage();
				doc.addPage(page);
				try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
					cs.beginText();
					cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
					cs.newLineAtOffset(50, 700);
					cs.showText(text);
					cs.endText();
				}
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			doc.save(out);
			return out.toByteArray();
		}
	}
}
