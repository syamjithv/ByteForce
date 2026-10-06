package com.byteforce.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailValidatorTest {

    @Test
    @DisplayName("Original valid email baseline test")
    void validEmailPasses() {
        assertTrue(EmailValidator.isValid("student@example.com"));
    }

    @Test
    @DisplayName("Original invalid email baseline test")
    void invalidEmailFails() {
        assertFalse(EmailValidator.isValid("studentexample.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "student@example.com",
            "syamjith@example.org",
            "john.doe@sub.domain.edu",
            "user_name@byteforce.io",
            "candidate+prep@tech-corp.com"
    })
    @DisplayName("Should accept properly formatted email addresses")
    void shouldAcceptValidEmails(String validEmail) {
        assertTrue(EmailValidator.isValid(validEmail), "Email should be valid: " + validEmail);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "@",
            "a@",
            "@gmail.com",
            "hello",
            "hello@",
            "hello @gmail.com",
            "hello@gmail",
            "hello@@gmail.com",
            "hello@.com",
            "hello@domain..com"
    })
    @DisplayName("Should reject malformed email addresses per specifications")
    void shouldRejectMalformedEmails(String invalidEmail) {
        assertFalse(EmailValidator.isValid(invalidEmail), "Email should be rejected: " + invalidEmail);
    }

    @Test
    @DisplayName("Should reject null, empty, and whitespace-only email strings")
    void shouldRejectNullAndEmptyEmails() {
        assertFalse(EmailValidator.isValid(null));
        assertFalse(EmailValidator.isValid(""));
        assertFalse(EmailValidator.isValid("   "));
    }

    @Test
    @DisplayName("normalize() should trim whitespace and convert to lowercase")
    void shouldNormalizeEmailCorrectly() {
        assertEquals("candidate@byteforce.com", EmailValidator.normalize("  Candidate@ByteForce.COM  "));
        assertNull(EmailValidator.normalize(null));
    }
}

