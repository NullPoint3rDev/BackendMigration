package org.alloy.security;

import java.util.List;

public class PasswordValidationException extends RuntimeException {
    private final List<String> validationErrors;

    public PasswordValidationException(String message, List<String> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
} 