package com.byteforce.exception;

public class ByteForceException extends RuntimeException {
    public ByteForceException(String message) {
        super(message);
    }

    public ByteForceException(String message, Throwable cause) {
        super(message, cause);
    }
}
