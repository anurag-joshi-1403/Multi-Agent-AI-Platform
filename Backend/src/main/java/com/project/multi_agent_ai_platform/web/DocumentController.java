package com.project.multi_agent_ai_platform.web;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.project.multi_agent_ai_platform.document.DocumentStore;
import com.project.multi_agent_ai_platform.document.DocumentTextExtractor;
import com.project.multi_agent_ai_platform.document.StoredDocument;
import com.project.multi_agent_ai_platform.web.dto.DocumentSummary;

/**
 * File uploads. The console uploads a file the moment it is attached, then sends only its id with
 * each message (in the {@code attachments} attribute).
 * <pre>
 *   POST   /api/documents        multipart "file" (PDF or text)  -> 201 + summary
 *   DELETE /api/documents/{id}   remove an upload (idempotent)   -> 204
 * </pre>
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

	private final DocumentStore store;

	private final DocumentTextExtractor extractor;

	public DocumentController(DocumentStore store, DocumentTextExtractor extractor) {
		this.store = store;
		this.extractor = extractor;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<DocumentSummary> upload(@RequestPart("file") MultipartFile file) throws IOException {
		String name = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
				? "upload"
				: file.getOriginalFilename();
		DocumentTextExtractor.Extracted extracted = extractor.extract(name, file.getContentType(), file.getBytes());
		StoredDocument doc = store.save(name, file.getContentType(), extracted.text(), extracted.pages());
		return ResponseEntity.created(URI.create("/api/documents/" + doc.id())).body(DocumentSummary.of(doc));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable String id) {
		store.delete(id);
		return ResponseEntity.noContent().build();
	}
}
