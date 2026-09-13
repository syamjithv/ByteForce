package com.byteforce.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    @DisplayName("Should create User successfully when all arguments are valid")
    void shouldCreateUserWithValidData() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        User user = new User(id, "student@byteforce.com", "$2a$12$hashvalue", "Alice Smith", Role.STUDENT, now, now);

        assertEquals(id, user.getId());
        assertEquals("student@byteforce.com", user.getEmail());
        assertEquals("$2a$12$hashvalue", user.getPasswordHash());
        assertEquals("Alice Smith", user.getFullName());
        assertEquals(Role.STUDENT, user.getRole());
        assertEquals(now, user.getCreatedAt());
        assertEquals(now, user.getUpdatedAt());
    }

    @Test
    @DisplayName("Factory method create() should initialize user with generated UUID and current timestamps")
    void shouldCreateUserViaFactoryMethod() {
        User user = User.create("admin@byteforce.com", "$2a$12$adminhash", "Admin User", Role.ADMIN);

        assertNotNull(user.getId());
        assertEquals("admin@byteforce.com", user.getEmail());
        assertEquals("Admin User", user.getFullName());
        assertEquals(Role.ADMIN, user.getRole());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    @DisplayName("Should normalize email by trimming and converting to lowercase")
    void shouldNormalizeEmail() {
        User user = User.create("  Student.Name@ByteForce.COM  ", "$2a$12$hash", "Student Name", Role.STUDENT);
        assertEquals("student.name@byteforce.com", user.getEmail());
    }

    @Test
    @DisplayName("Should trim full name whitespace")
    void shouldTrimFullName() {
        User user = User.create("test@example.com", "$2a$12$hash", "   Bob Marley   ", Role.STUDENT);
        assertEquals("Bob Marley", user.getFullName());
    }

    @Test
    @DisplayName("Should reject null or blank required attributes")
    void shouldRejectInvalidInvariants() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        assertThrows(NullPointerException.class,
                () -> new User(null, "user@test.com", "hash", "Name", Role.STUDENT, now, now));

        assertThrows(NullPointerException.class,
                () -> new User(id, null, "hash", "Name", Role.STUDENT, now, now));
        assertThrows(IllegalArgumentException.class,
                () -> new User(id, "   ", "hash", "Name", Role.STUDENT, now, now));

        assertThrows(NullPointerException.class,
                () -> new User(id, "user@test.com", null, "Name", Role.STUDENT, now, now));
        assertThrows(IllegalArgumentException.class,
                () -> new User(id, "user@test.com", "   ", "Name", Role.STUDENT, now, now));

        assertThrows(NullPointerException.class,
                () -> new User(id, "user@test.com", "hash", null, Role.STUDENT, now, now));
        assertThrows(IllegalArgumentException.class,
                () -> new User(id, "user@test.com", "hash", "   ", Role.STUDENT, now, now));

        assertThrows(NullPointerException.class,
                () -> new User(id, "user@test.com", "hash", "Name", null, now, now));

        assertThrows(NullPointerException.class,
                () -> new User(id, "user@test.com", "hash", "Name", Role.STUDENT, null, now));

        assertThrows(NullPointerException.class,
                () -> new User(id, "user@test.com", "hash", "Name", Role.STUDENT, now, null));
    }

    @Test
    @DisplayName("withFullName(), withRole(), and withPasswordHash() should return updated immutable copies")
    void shouldSupportImmutableWithers() {
        User original = User.create("user@byteforce.com", "hash1", "Initial Name", Role.STUDENT);

        User updatedName = original.withFullName("New Name");
        assertEquals("New Name", updatedName.getFullName());
        assertEquals(original.getId(), updatedName.getId());
        assertEquals(original.getEmail(), updatedName.getEmail());

        User updatedRole = original.withRole(Role.ADMIN);
        assertEquals(Role.ADMIN, updatedRole.getRole());
        assertEquals(original.getId(), updatedRole.getId());

        User updatedPass = original.withPasswordHash("newHash2");
        assertEquals("newHash2", updatedPass.getPasswordHash());
        assertEquals(original.getId(), updatedPass.getId());
    }

    @Test
    @DisplayName("Equality and hashCode should be based on entity ID identity")
    void shouldEvaluateEqualityBasedOnId() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        User user1 = new User(id, "u1@test.com", "hash1", "Name 1", Role.STUDENT, now, now);
        User user2 = new User(id, "u2@test.com", "hash2", "Name 2", Role.ADMIN, now, now);
        User user3 = new User(UUID.randomUUID(), "u1@test.com", "hash1", "Name 1", Role.STUDENT, now, now);

        assertEquals(user1, user2, "Users with same ID must be equal");
        assertEquals(user1.hashCode(), user2.hashCode());
        assertNotEquals(user1, user3, "Users with different IDs must not be equal");
        assertNotEquals(user1, null);
        assertNotEquals(user1, "other-type");
    }

    @Test
    @DisplayName("toString() must NEVER expose the password hash")
    void toStringMustNeverIncludePasswordHash() {
        String sensitiveHash = "$2a$12$superSecretHashValueToHide";
        User user = User.create("secret@test.com", sensitiveHash, "John Secret", Role.STUDENT);

        String str = user.toString();
        assertFalse(str.contains(sensitiveHash), "Password hash must be excluded from toString()");
        assertTrue(str.contains("secret@test.com"));
        assertTrue(str.contains("John Secret"));
        assertTrue(str.contains("STUDENT"));
    }
}
