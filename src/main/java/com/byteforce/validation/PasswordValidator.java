package com.byteforce.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates password strength according to educational and placement preparation application standards.
 * Rules:
 * - Not null or blank
 * - Minimum 8 characters, maximum 128 characters
 * - At least one uppercase letter (A-Z)
 * - At least one lowercase letter (a-z)
 * - At least one digit (0-9)
 * - At least one special character
 * - No whitespace characters
 */
public final class PasswordValidator {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 128;

    private PasswordValidator() {
    }

    /**
     * Represents the outcome of password validation, containing detailed error messages if invalid.
     */
    public record ValidationResult(boolean valid, List<String> errors) {
        public static ValidationResult success() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, List.copyOf(errors));
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }

    /**
     * Validates a raw password and returns a detailed ValidationResult.
     */
    public static ValidationResult validate(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            errors.add("Password must not be empty");
            return ValidationResult.failure(errors);
        }

        if (password.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters long");
        }

        if (password.length() > MAX_LENGTH) {
            errors.add("Password must not exceed " + MAX_LENGTH + " characters");
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        boolean hasWhitespace = false;

        for (char c : password.toCharArray()) {
            if (Character.isWhitespace(c)) {
                hasWhitespace = true;
            } else if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else {
                // Any character that is not alphanumeric and not whitespace counts as special
                hasSpecial = true;
            }
        }

        if (hasWhitespace) {
            errors.add("Password must not contain whitespace characters");
        }
        if (!hasUpper) {
            errors.add("Password must contain at least one uppercase letter (A-Z)");
        }
        if (!hasLower) {
            errors.add("Password must contain at least one lowercase letter (a-z)");
        }
        if (!hasDigit) {
            errors.add("Password must contain at least one digit (0-9)");
        }
        if (!hasSpecial) {
            errors.add("Password must contain at least one special character (e.g. !@#$%^&*)");
        }

        if (errors.isEmpty()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(errors);
    }

    /**
     * Convenience method returning true if the password passes all validation rules.
     */
    public static boolean isValid(String password) {
        return validate(password).valid();
    }
}
