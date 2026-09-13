package com.byteforce.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Production-grade BCrypt password hasher enforcing work factor >= 12.
 */
public final class BCryptPasswordHasher implements PasswordHasher {

    public static final int DEFAULT_WORK_FACTOR = 12;

    private final int workFactor;

    public BCryptPasswordHasher() {
        this(DEFAULT_WORK_FACTOR);
    }

    public BCryptPasswordHasher(int workFactor) {
        if (workFactor < 12 || workFactor > 31) {
            throw new IllegalArgumentException("BCrypt work factor must be between 12 and 31. Provided: " + workFactor);
        }
        this.workFactor = workFactor;
    }

    @Override
    public String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password must not be null or blank");
        }
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(workFactor));
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        if (rawPassword == null || rawPassword.isBlank() || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, passwordHash);
        } catch (IllegalArgumentException e) {
            // Handled safely: malformed or invalid BCrypt hash returns false instead of crashing
            return false;
        }
    }

    public int getWorkFactor() {
        return workFactor;
    }
}
