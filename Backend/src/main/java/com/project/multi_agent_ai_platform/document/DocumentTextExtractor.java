package com.project.multi_agent_ai_platform.document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * Turns an uploaded file into plain text. PDFs go through PDFBox page by page so the agent can
 * cite page numbers; everything text-like is decoded as UTF-8.
 */
@Component
public class DocumentTextExtractor {

	/**
	 * @param text  extracted text
	 * @param pages page count, or {@code null} for non-paged formats
	 */
	public record Extracted(String text, Integer pages) {
	}

	/** Every extension the console's file picker offers, minus {@code pdf}. */
	private static final Set<String> TEXT_EXTENSIONS = Set.of("txt", "md", "markdown", "csv", "json", "xml", "html",
			"htm", "yaml", "yml", "log", "java", "ts", "tsx", "js", "jsx", "py", "kt", "go", "rs", "c", "cpp", "h",
			"cs", "sql", "properties", "toml", "ini");

	private static final char REPLACEMENT_CHAR = '�';

	public Extracted extract(String fileName, String mediaType, byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			throw new UnsupportedDocumentException("The uploaded file is empty.");
		}
		String type = mediaType == null ? "" : mediaType.toLowerCase(Locale.ROOT);
		String ext = extension(fileName);

		if (type.contains("pdf") || "pdf".equals(ext) || looksLikePdf(bytes)) {
			return extractPdf(bytes);
		}
		// Browsers guess media types from the extension, and badly for code: on Windows a .ts file
		// arrives as video/mp2t. So a known text extension wins over the reported type.
		boolean textLike = type.startsWith("text/") || type.contains("json") || type.contains("xml")
				|| TEXT_EXTENSIONS.contains(ext) || type.isEmpty() || type.equals("application/octet-stream");
		if (textLike) {
			String text = new String(bytes, StandardCharsets.UTF_8);
			long bad = text.chars().filter(c -> c == REPLACEMENT_CHAR).count();
			if (bad > 0 && bad > text.length() / 50) {
				throw new UnsupportedDocumentException(
						"The file does not look like UTF-8 text. Supported: PDF and plain-text formats.");
			}
			if (text.isBlank()) {
				throw new UnsupportedDocumentException("The uploaded file contains no text.");
			}
			return new Extracted(text, null);
		}
		throw new UnsupportedDocumentException("Unsupported file type '" + mediaType
				+ "'. Supported: PDF and plain-text formats (txt, md, csv, json, code, ...).");
	}

	private static Extracted extractPdf(byte[] bytes) {
		try (PDDocument pdf = Loader.loadPDF(bytes)) {
			int pages = pdf.getNumberOfPages();
			PDFTextStripper stripper = new PDFTextStripper();
			stripper.setSortByPosition(true);
			StringBuilder out = new StringBuilder();
			for (int page = 1; page <= pages; page++) {
				stripper.setStartPage(page);
				stripper.setEndPage(page);
				String text = stripper.getText(pdf).strip();
				if (!text.isEmpty()) {
					out.append("[page ").append(page).append("]\n").append(text).append("\n\n");
				}
			}
			String text = out.toString().strip();
			if (text.isEmpty()) {
				throw new UnsupportedDocumentException(
						"The PDF contains no extractable text (it may be scanned images). OCR is not supported.");
			}
			return new Extracted(text, pages);
		}
		catch (IOException ex) {
			throw new UnsupportedDocumentException("Could not read the PDF: " + ex.getMessage(), ex);
		}
	}

	private static boolean looksLikePdf(byte[] bytes) {
		return bytes.length > 4 && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F';
	}

	private static String extension(String fileName) {
		if (fileName == null) {
			return "";
		}
		int dot = fileName.lastIndexOf('.');
		return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
	}
}
