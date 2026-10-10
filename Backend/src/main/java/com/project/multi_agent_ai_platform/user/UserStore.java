package com.project.multi_agent_ai_platform.user;

import java.util.Optional;

/**
 * Where console accounts live. {@link MongoUserStore} is the implementation; nothing above this
 * interface - not {@code AuthController}, not {@link DatabaseUserDetailsService} - depends on it.
 */
public interface UserStore {

	Optional<UserAccount> findByUsername(String username);

	/**
	 * Save a new account with role {@code USER}.
	 *
	 * @param passwordHash already hashed; never the password itself
	 * @throws UsernameTakenException when the username is already registered
	 */
	UserAccount create(String username, String passwordHash);
}
