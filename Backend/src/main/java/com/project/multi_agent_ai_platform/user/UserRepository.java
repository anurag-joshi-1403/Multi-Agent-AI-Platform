package com.project.multi_agent_ai_platform.user;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

/** Spring Data writes the code for these methods from their names. */
public interface UserRepository extends MongoRepository<UserDocument, String> {

	Optional<UserDocument> findByUsername(String username);

	boolean existsByUsername(String username);
}
