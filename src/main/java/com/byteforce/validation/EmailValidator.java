package com.byteforce.validation;

public final class EmailValidator {

    private EmailValidator() {
    }

    public static boolean isValid(String email) {
        return email != null && email.contains("@");
    }
}
