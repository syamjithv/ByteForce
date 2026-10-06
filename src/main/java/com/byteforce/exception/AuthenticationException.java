package com.byteforce.exception;

/**
 * Thrown when an authentication attempt fails (e.g., invalid credentials, unknown user, unauthenticated session).
 */
public class AuthenticationException extends ByteForceException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
