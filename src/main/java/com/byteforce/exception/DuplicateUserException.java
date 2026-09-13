package com.byteforce.exception;

/**
 * Thrown when attempting to register or save a user with an already existing email.
 */
public class DuplicateUserException extends ByteForceException {

    public DuplicateUserException(String message) {
        super(message);
    }

    public DuplicateUserException(String message, Throwable cause) {
        super(message, cause);
    }
}
