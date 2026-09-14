package com.byteforce.exception;

/**
 * Thrown when a requested resource (e.g. Question, Topic, User) cannot be found.
 */
public class ResourceNotFoundException extends ByteForceException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
