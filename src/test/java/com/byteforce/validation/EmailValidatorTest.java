package com.byteforce.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EmailValidatorTest {

    @Test
    void validEmailPasses() {
        assertTrue(EmailValidator.isValid("student@example.com"));
    }

    @Test
    void invalidEmailFails() {
        assertFalse(EmailValidator.isValid("studentexample.com"));
    }
}
