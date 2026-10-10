package com.project.multi_agent_ai_platform.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/** No database: a store that knows one account. */
class DatabaseUserDetailsServiceTest {

	private final UserStore oneAccount = new UserStore() {
		@Override
		public Optional<UserAccount> findByUsername(String username) {
			return "anna".equals(username)
					? Optional.of(new UserAccount("id-1", "anna", "$2a$10$hash", "ADMIN", Instant.now()))
					: Optional.empty();
		}

		@Override
		public UserAccount create(String username, String passwordHash) {
			throw new UnsupportedOperationException();
		}
	};

	private final DatabaseUserDetailsService service = new DatabaseUserDetailsService(oneAccount);

	@Test
	void turnsTheStoredAccountIntoLoginDetails() {
		UserDetails anna = service.loadUserByUsername("anna");

		assertThat(anna.getUsername()).isEqualTo("anna");
		assertThat(anna.getPassword()).isEqualTo("$2a$10$hash");
		assertThat(anna.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
	}

	@Test
	void anUnknownUsernameIsNotFound() {
		assertThatThrownBy(() -> service.loadUserByUsername("nobody")).isInstanceOf(UsernameNotFoundException.class);
	}
}
