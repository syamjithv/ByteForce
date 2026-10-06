package com.byteforce.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BCryptPasswordHasherTest {

    private BCryptPasswordHasher hasher;

    @BeforeEach
    void setUp() {
        hasher = new BCryptPasswordHasher();
    }

    @Test
    @DisplayName("Hash must differ from plaintext password and use work factor >= 12")
    void shouldProduceValidBCryptHash() {
        String rawPassword = "CandidateSecurePassword#2026";
        String hash = hasher.hash(rawPassword);

        assertNotNull(hash);
        assertNotEquals(rawPassword, hash, "Hash must never equal raw password");
        assertTrue(hash.startsWith("$2a$12$") || hash.startsWith("$2y$12$"),
                "Hash must start with standard BCrypt prefix with work factor 12. Actual: " + hash);
        assertEquals(12, hasher.getWorkFactor());
    }

    @Test
    @DisplayName("matches() should return true for correct password and false for incorrect password")
    void shouldVerifyPasswordsAccurately() {
        String raw = "MyPlacementPrep@99";
        String hash = hasher.hash(raw);

        assertTrue(hasher.matches(raw, hash), "Correct password must match its hash");
        assertFalse(hasher.matches("WrongPassword@99", hash), "Incorrect password must not match");
        assertFalse(hasher.matches("myplacementprep@99", hash), "Password matching must be case sensitive");
    }

    @Test
    @DisplayName("Two hashes of the same password must differ due to unique cryptographic salt")
    void shouldGenerateUniqueSalts() {
        String raw = "IdenticalRawPassword!123";

        String hash1 = hasher.hash(raw);
        String hash2 = hasher.hash(raw);

        assertNotEquals(hash1, hash2, "BCrypt must generate different hashes for the same password due to random salt");
        assertTrue(hasher.matches(raw, hash1));
        assertTrue(hasher.matches(raw, hash2));
    }

    @Test
    @DisplayName("hash() must reject null or blank raw passwords")
    void shouldRejectInvalidRawPasswordsOnHash() {
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(null));
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(""));
        assertThrows(IllegalArgumentException.class, () -> hasher.hash("   "));
    }

    @Test
    @DisplayName("matches() must handle null, empty, or malformed hashes safely without throwing exceptions")
    void shouldHandleMalformedHashesSafely() {
        assertFalse(hasher.matches(null, "$2a$12$somevalidlengthhashvalue"));
        assertFalse(hasher.matches("pass", null));
        assertFalse(hasher.matches("", "$2a$12$somevalidlengthhashvalue"));
        assertFalse(hasher.matches("pass", ""));
        assertFalse(hasher.matches("pass", "not-a-valid-bcrypt-hash"));
    }

    @Test
    @DisplayName("Constructor must reject work factor less than 12")
    void shouldEnforceMinimumWorkFactor() {
        assertThrows(IllegalArgumentException.class, () -> new BCryptPasswordHasher(11));
        assertThrows(IllegalArgumentException.class, () -> new BCryptPasswordHasher(32));
    }
}
