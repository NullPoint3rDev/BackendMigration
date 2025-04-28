package org.alloy.security;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class PasswordValidationService {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern HAS_UPPER = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWER = Pattern.compile("[a-z]");
    private static final Pattern HAS_NUMBER = Pattern.compile("\\d");
    private static final Pattern HAS_SPECIAL = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");

    public List<String> validatePassword(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            errors.add("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }

        if (!HAS_UPPER.matcher(password).find()) {
            errors.add("Password must contain at least one uppercase letter");
        }

        if (!HAS_LOWER.matcher(password).find()) {
            errors.add("Password must contain at least one lowercase letter");
        }

        if (!HAS_NUMBER.matcher(password).find()) {
            errors.add("Password must contain at least one number");
        }

        if (!HAS_SPECIAL.matcher(password).find()) {
            errors.add("Password must contain at least one special character");
        }

        return errors;
    }

    public boolean isPasswordValid(String password) {
        return validatePassword(password).isEmpty();
    }
} 