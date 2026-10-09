package com.project.multi_agent_ai_platform.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.project.multi_agent_ai_platform.agent.core.AgentRequest;
import com.project.multi_agent_ai_platform.config.PlatformProperties;

class AttachmentResolverTest {

	private final FakeDocumentStore store = new FakeDocumentStore();

	private AttachmentResolver resolver(int maxContextChars) {
		return new AttachmentResolver(store,
				new PlatformProperties(new PlatformProperties.Memory(20),
						new PlatformProperties.Documents(maxContextChars, 50), new PlatformProperties.Auth("")));
	}

	private static AgentRequest withAttachments(Object value) {
		return new AgentRequest(null, "q", Map.of(AttachmentResolver.ATTRIBUTE, value));
	}

	@Test
	void resolvesIdsInOrderWithoutDuplicatesAndSkipsUnknownOnes() {
		StoredDocument a = store.save("a.txt", "text/plain", "A", null);
		StoredDocument b = store.save("b.txt", "text/plain", "B", null);

		List<StoredDocument> resolved = resolver(1000)
			.resolve(withAttachments(Arrays.asList(b.id(), "doc_gone", a.id(), b.id(), " ", null)));

		assertThat(resolved).containsExactly(b, a);
	}

	@Test
	void acceptsASingleIdAndNothingAtAll() {
		StoredDocument a = store.save("a.txt", "text/plain", "A", null);

		assertThat(resolver(1000).resolve(withAttachments(a.id()))).containsExactly(a);
		assertThat(resolver(1000).resolve(AgentRequest.of("q"))).isEmpty();
	}

	@Test
	void blockCarriesEachFileByName() {
		StoredDocument a = store.save("a.txt", "text/plain", "alpha text", null);

		String block = resolver(1000).block(List.of(a));

		assertThat(block).contains("--- FILE: a.txt ---\nalpha text\n--- END OF FILE ---");
		assertThat(resolver(1000).block(List.of())).isNull();
	}

	@Test
	void theCharacterBudgetIsSharedEvenly() {
		StoredDocument big = store.save("big.txt", "text/plain", "x".repeat(100), null);
		StoredDocument small = store.save("small.txt", "text/plain", "short", null);

		String block = resolver(40).block(List.of(big, small));

		assertThat(block).contains("--- FILE: big.txt (truncated) ---\n" + "x".repeat(20) + "\n");
		assertThat(block).contains("--- FILE: small.txt ---\nshort\n");
	}
}
