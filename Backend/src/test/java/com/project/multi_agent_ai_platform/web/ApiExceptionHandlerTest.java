package com.project.multi_agent_ai_platform.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.unit.DataSize;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * A too-large upload never reaches a controller (the multipart parser rejects it first), so the
 * handler is called directly here; the real 413 is checked against a running server.
 */
class ApiExceptionHandlerTest {

	private static ProblemDetail tooLarge(DataSize limit) {
		ResponseEntity<Object> response = new ApiExceptionHandler(limit).handleMaxUploadSizeExceededException(
				new MaxUploadSizeExceededException(-1), new HttpHeaders(), HttpStatus.valueOf(413),
				new ServletWebRequest(new MockHttpServletRequest()));
		assertThat(response.getStatusCode().value()).isEqualTo(413);
		return (ProblemDetail) response.getBody();
	}

	@Test
	void aTooLargeFileIsToldTheLimit() {
		ProblemDetail problem = tooLarge(DataSize.ofMegabytes(20));

		assertThat(problem.getTitle()).isEqualTo("File too large");
		assertThat(problem.getDetail()).isEqualTo("The file is larger than 20 MB. Attach a smaller one.");
	}

	@Test
	void aLimitUnderOneMegabyteIsShownInKilobytes() {
		assertThat(tooLarge(DataSize.ofKilobytes(512)).getDetail()).startsWith("The file is larger than 512 KB.");
	}
}
