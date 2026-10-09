package com.project.multi_agent_ai_platform.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class SecurityConfigTest {

	private final SecurityConfig config = new SecurityConfig();

	private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

	private static PlatformProperties users(String raw) {
		return new PlatformProperties(new PlatformProperties.Memory(20), new PlatformProperties.Documents(60_000, 50),
				new PlatformProperties.Auth(raw));
	}

	@Test
	void parsesUsernamePasswordPairs() {
		assertThat(SecurityConfig.parseUsers(" anna:s3cret , ben:pa:ss ,"))
			.containsExactly(new SecurityConfig.Credential("anna", "s3cret"), new SecurityConfig.Credential("ben", "pa:ss"));
		assertThat(SecurityConfig.parseUsers("")).isEmpty();
		assertThat(SecurityConfig.parseUsers(null)).isEmpty();
	}

	@Test
	void malformedEntriesStopStartupWithoutPrintingThePassword() {
		assertThatThrownBy(() -> SecurityConfig.parseUsers("anna"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("username:password");
		assertThatThrownBy(() -> SecurityConfig.parseUsers("anna:"))
			.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> SecurityConfig.parseUsers(":secret-value"))
			.isInstanceOf(IllegalStateException.class)
			.message()
			.doesNotContain("secret-value");
	}

	@Test
	void aRepeatedUsernameStopsStartup() {
		assertThatThrownBy(() -> SecurityConfig.parseUsers("anna:a,anna:b"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("more than once");
	}

	@Test
	void configuredAccountsAreStoredHashed() {
		UserDetailsService service = config.userDetailsService(users("anna:s3cret"), encoder);

		UserDetails anna = service.loadUserByUsername("anna");
		assertThat(anna.getPassword()).isNotEqualTo("s3cret");
		assertThat(encoder.matches("s3cret", anna.getPassword())).isTrue();
	}

	@Test
	void withNoAccountsThereIsAnAdminWithARandomPasswordNotADefaultOne() {
		UserDetails admin = config.userDetailsService(users(""), encoder).loadUserByUsername("admin");

		assertThat(encoder.matches("admin", admin.getPassword())).isFalse();
		assertThat(encoder.matches("", admin.getPassword())).isFalse();
	}
}
