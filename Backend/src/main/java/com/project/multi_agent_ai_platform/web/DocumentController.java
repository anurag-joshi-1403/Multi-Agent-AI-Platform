package com.project.multi_agent_ai_platform.web;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.project.multi_agent_ai_platform.document.DocumentStore;
import com.project.multi_agent_ai_platform.document.DocumentTextExtractor;
import com.project.multi_agent_ai_platform.document.StoredDocument;
import com.project.multi_agent_ai_platform.document.UnsupportedDocumentException;
import com.project.multi_agent_ai_platform.web.dto.DocumentSummary;
import com.project.multi_agent_ai_platform.web.dto.TextDocumentRequest;

import jakarta.validation.Valid;

/**
 * Upload documents for the document agent.
 * <pre>
 *   POST   /api/documents               multipart "file" (PDF or text)  -> 201 + summary
 *   POST   /api/documents/text          JSON {name, content}            -> 201 + summary
 *   GET    /api/documents               list summaries (newest first)
 *   GET    /api/documents/{id}          summary
 *   GET    /api/documents/{id}/content  extracted text as text/plain
 *   DELETE /api/documents/{id}          204
 * </pre>
 */
@RestController
@RequestMapping(path = "/api/documents")
public class DocumentController {

	private final DocumentStore store;

	private final DocumentTextExtractor extractor;

	public DocumentController(DocumentStore store, DocumentTextExtractor extractor) {
		this.store = store;
		this.extractor = extractor;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<DocumentSummary> upload(@RequestPart("file") MultipartFile file) throws IOException {
		if (file.isEmpty()) {
			throw new UnsupportedDocumentException("The uploaded file is empty.");
		}
		String name = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
				? "upload"
				: file.getOriginalFilename();
		DocumentTextExtractor.Extracted extracted = extractor.extract(name, file.getContentType(), file.getBytes());
		StoredDocument doc = store.save(name, file.getContentType(), extracted.text(), extracted.pages());
		return created(doc);
	}

	@PostMapping(path = "/text", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<DocumentSummary> paste(@Valid @RequestBody TextDocumentRequest body) {
		String name = body.name() == null || body.name().isBlank() ? "pasted-text.txt" : body.name();
		StoredDocument doc = store.save(name, MediaType.TEXT_PLAIN_VALUE, body.content(), null);
		return created(doc);
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public List<DocumentSummary> list() {
		return store.all().stream().map(DocumentSummary::of).toList();
	}

	@GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public DocumentSummary get(@PathVariable String id) {
		return DocumentSummary.of(store.get(id));
	}

	@GetMapping(path = "/{id}/content", produces = MediaType.TEXT_PLAIN_VALUE)
	public String content(@PathVariable String id) {
		return store.get(id).content();
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable String id) {
		store.delete(id);
		return ResponseEntity.noContent().build();
	}

	private static ResponseEntity<DocumentSummary> created(StoredDocument doc) {
		return ResponseEntity.created(URI.create("/api/documents/" + doc.id())).body(DocumentSummary.of(doc));
	}
}
