package com.byteforce.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "ByteForce#2026",
            "Secur3!Pass",
            "P@ssw0rd123",
            "C0d!ngPrep_2026",
            "Java21$Awesome!"
    })
    @DisplayName("Should accept passwords satisfying all complexity requirements")
    void shouldAcceptValidPasswords(String password) {
        assertTrue(PasswordValidator.isValid(password), "Password should be valid: " + password);
        PasswordValidator.ValidationResult result = PasswordValidator.validate(password);
        assertTrue(result.valid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    @DisplayName("Should reject password shorter than minimum length (8 chars)")
    void shouldRejectTooShortPassword() {
        PasswordValidator.ValidationResult result = PasswordValidator.validate("Aa1!xyz");
        assertFalse(result.valid());
        assertTrue(result.getErrorMessage().contains("at least 8 characters"));
    }

    @Test
    @DisplayName("Should reject password missing uppercase character")
    void shouldRejectMissingUppercase() {
        PasswordValidator.ValidationResult result = PasswordValidator.validate("byteforce#2026");
        assertFalse(result.valid());
        assertTrue(result.getErrorMessage().contains("uppercase"));
    }

    @Test
    @DisplayName("Should reject password missing lowercase character")
    void shouldRejectMissingLowercase() {
        PasswordValidator.ValidationResult result = PasswordValidator.validate("BYTEFORCE#2026");
        assertFalse(result.valid());
        assertTrue(result.getErrorMessage().contains("lowercase"));
    }

    @Test
    @DisplayName("Should reject password missing digit")
    void shouldRejectMissingDigit() {
        PasswordValidator.ValidationResult result = PasswordValidator.validate("ByteForce#Password");
        assertFalse(result.valid());
        assertTrue(result.getErrorMessage().contains("digit"));
    }

    @Test
    @DisplayName("Should reject password missing special character")
    void shouldRejectMissingSpecialChar() {
        PasswordValidator.ValidationResult result = PasswordValidator.validate("ByteForce2026");
        assertFalse(result.valid());
        assertTrue(result.getErrorMessage().contains("special character"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ByteForce #2026",
            " ByteForce#2026",
            "ByteForce#2026 ",
            "Byte\tForce#2026"
    })
    @DisplayName("Should reject passwords containing whitespace")
    void shouldRejectPasswordsWithWhitespace(String passwordWithSpace) {
        PasswordValidator.ValidationResult result = PasswordValidator.validate(passwordWithSpace);
        assertFalse(result.valid());
        assertTrue(result.getErrorMessage().contains("whitespace"));
    }

    @Test
    @DisplayName("Should reject null and empty passwords")
    void shouldRejectNullAndEmptyPasswords() {
        assertFalse(PasswordValidator.isValid(null));
        assertFalse(PasswordValidator.isValid(""));
    }

    @Test
    @DisplayName("Should reject passwords exceeding 128 characters")
    void shouldRejectOverlyLongPassword() {
        String longPassword = "A1!" + "a".repeat(130);
        PasswordValidator.ValidationResult result = PasswordValidator.validate(longPassword);
        assertFalse(result.valid());
        assertTrue(result.getErrorMessage().contains("must not exceed 128"));
    }
}
