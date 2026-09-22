package com.project.multi_agent_ai_platform.document;

import java.security.SecureRandom;

/**
 * Document ids, shared by every {@link DocumentStore} implementation so an id keeps the same shape
 * — {@code doc_} plus 12 lowercase alphanumerics — whichever store produced it. Ids reach the
 * client and come back as request attributes, so they are URL-safe and not sequential.
 */
final class DocumentIds {

	private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

	private static final SecureRandom RANDOM = new SecureRandom();

	private DocumentIds() {
	}

	static String newId() {
		StringBuilder sb = new StringBuilder("doc_");
		for (int i = 0; i < 12; i++) {
			sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
		}
		return sb.toString();
	}
}
