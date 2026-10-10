package com.project.multi_agent_ai_platform.user;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * What Spring Security calls on every login: find the account in the database. The
 * {@code AuthenticationManager} in {@code SecurityConfig} then checks the password against its
 * BCrypt hash.
 */
@Component
public class DatabaseUserDetailsService implements UserDetailsService {

	private final UserStore users;

	public DatabaseUserDetailsService(UserStore users) {
		this.users = users;
	}

	@Override
	public UserDetails loadUserByUsername(String username) {
		UserAccount account = users.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));
		return User.withUsername(account.username())
			.password(account.passwordHash())
			.roles(account.role())
			.build();
	}
}
