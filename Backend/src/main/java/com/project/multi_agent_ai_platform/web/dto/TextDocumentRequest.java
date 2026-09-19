package com.project.multi_agent_ai_platform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of {@code POST /api/documents/text}: paste text instead of uploading a file. */
public record TextDocumentRequest(
		@Size(max = 200) String name,
		@NotBlank(message = "content must not be blank") @Size(max = 2_000_000) String content) {
}
