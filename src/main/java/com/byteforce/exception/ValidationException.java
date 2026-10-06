package com.byteforce.exception;

import java.util.List;

/**
 * Thrown when domain or input validation rules are violated.
 */
public class ValidationException extends ByteForceException {

    private final List<String> errors;

    public ValidationException(String message) {
        super(message);
        this.errors = List.of(message);
    }

    public ValidationException(String message, List<String> errors) {
        super(message);
        this.errors = errors != null ? List.copyOf(errors) : List.of(message);
    }

    public List<String> getErrors() {
        return errors;
    }
}
