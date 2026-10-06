package com.byteforce.validation;

import java.util.regex.Pattern;

/**
 * Validates email addresses using a robust and maintainable format check.
 */
public final class EmailValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    private EmailValidator() {
    }

    /**
     * Checks if the given email string has a valid email address format.
     */
    public static boolean isValid(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Normalizes a valid email address by trimming whitespace and converting to lowercase.
     * Returns null if the email is null.
     */
    public static String normalize(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}

