package com.project.multi_agent_ai_platform.document;

/** The upload is a file type we cannot extract text from, or is unreadable. Mapped to HTTP 415. */
public class UnsupportedDocumentException extends RuntimeException {

	public UnsupportedDocumentException(String message) {
		super(message);
	}

	public UnsupportedDocumentException(String message, Throwable cause) {
		super(message, cause);
	}
}
