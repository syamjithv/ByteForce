package com.byteforce.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable domain entity representing a user in ByteForce.
 */
public final class User {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final String fullName;
    private final Role role;
    private final Instant createdAt;
    private final Instant updatedAt;

    public User(UUID id, String email, String passwordHash, String fullName, Role role, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");

        Objects.requireNonNull(email, "email must not be null");
        if (email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        this.email = email.trim().toLowerCase();

        Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        if (passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }
        this.passwordHash = passwordHash.trim();

        Objects.requireNonNull(fullName, "fullName must not be null");
        if (fullName.isBlank()) {
            throw new IllegalArgumentException("fullName must not be blank");
        }
        this.fullName = fullName.trim();

        this.role = Objects.requireNonNull(role, "role must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    /**
     * Factory method to create a new user with a generated UUID and current timestamps.
     */
    public static User create(String email, String passwordHash, String fullName, Role role) {
        Instant now = Instant.now();
        return new User(UUID.randomUUID(), email, passwordHash, fullName, role, now, now);
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Returns a copy of this user with updated full name and current updatedAt timestamp.
     */
    public User withFullName(String newFullName) {
        return new User(this.id, this.email, this.passwordHash, newFullName, this.role, this.createdAt, Instant.now());
    }

    /**
     * Returns a copy of this user with updated role and current updatedAt timestamp.
     */
    public User withRole(Role newRole) {
        return new User(this.id, this.email, this.passwordHash, this.fullName, newRole, this.createdAt, Instant.now());
    }

    /**
     * Returns a copy of this user with updated password hash and current updatedAt timestamp.
     */
    public User withPasswordHash(String newPasswordHash) {
        return new User(this.id, this.email, newPasswordHash, this.fullName, this.role, this.createdAt, Instant.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role=" + role +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

