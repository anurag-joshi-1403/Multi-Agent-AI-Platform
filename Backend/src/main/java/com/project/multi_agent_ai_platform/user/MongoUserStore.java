package com.project.multi_agent_ai_platform.user;

import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

/** Accounts in MongoDB ({@code users} collection, database from {@code MONGODB_URI}). */
@Component
public class MongoUserStore implements UserStore {

	private final UserRepository repository;

	public MongoUserStore(UserRepository repository) {
		this.repository = repository;
	}

	@Override
	public Optional<UserAccount> findByUsername(String username) {
		return repository.findByUsername(username).map(MongoUserStore::toAccount);
	}

	@Override
	public UserAccount create(String username, String passwordHash) {
		// The check gives a clear answer in the common case; the unique index on username is what
		// stops two sign-ups with the same name that arrive at the same moment.
		if (repository.existsByUsername(username)) {
			throw new UsernameTakenException(username);
		}
		UserDocument doc = new UserDocument();
		doc.setUsername(username);
		doc.setPasswordHash(passwordHash);
		try {
			return toAccount(repository.save(doc));
		}
		catch (DuplicateKeyException ex) {
			throw new UsernameTakenException(username);
		}
	}

	private static UserAccount toAccount(UserDocument d) {
		return new UserAccount(d.getId(), d.getUsername(), d.getPasswordHash(), d.getRole(), d.getCreatedAt());
	}
}
