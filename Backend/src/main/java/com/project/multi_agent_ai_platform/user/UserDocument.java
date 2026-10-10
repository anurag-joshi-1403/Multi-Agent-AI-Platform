package com.project.multi_agent_ai_platform.user;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * One account in the MongoDB {@code users} collection. The unique index on {@code username} is
 * created on startup ({@code spring.data.mongodb.auto-index-creation=true}), so the database itself
 * refuses a second account with the same name.
 */
@Document("users")
public class UserDocument {

	@Id
	private String id;

	@Indexed(unique = true)
	private String username;

	private String passwordHash;

	private String role = "USER";

	private Instant createdAt = Instant.now();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
