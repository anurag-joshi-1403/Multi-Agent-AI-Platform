package com.project.multi_agent_ai_platform.user;

import java.time.Instant;

/**
 * One console account, as stored in the database.
 *
 * @param id           database id
 * @param username     unique sign-in name
 * @param passwordHash BCrypt hash of the password; the password itself is never stored
 * @param role         {@code USER}, {@code ADMIN}… (without the {@code ROLE_} prefix)
 * @param createdAt    when the account was created
 */
public record UserAccount(String id, String username, String passwordHash, String role, Instant createdAt) {
}
