package com.project.multi_agent_ai_platform.web.dto;

import jakarta.validation.constraints.Min;

/** Body of POST /api/conversations/{id}/rewind: how many of the latest turns to forget. */
public record RewindRequest(@Min(value = 1, message = "at least 1") int turns) {
}
