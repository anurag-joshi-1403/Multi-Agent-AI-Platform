package com.project.multi_agent_ai_platform.document;

/** No document with the given id is held in the store. Mapped to HTTP 404. */
public class DocumentNotFoundException extends RuntimeException {

	private final String documentId;

	public DocumentNotFoundException(String documentId) {
		super("No document with id " + documentId + ". It may have been evicted; upload it again.");
		this.documentId = documentId;
	}

	public String getDocumentId() {
		return documentId;
	}
}
