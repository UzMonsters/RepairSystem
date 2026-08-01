package com.example.darks.repair_auto.auth.service;

import com.example.darks.repair_auto.common.error.BusinessRuleException;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 128;

    public void validate(String password, String normalizedEmail) {
        if (password == null || password.isBlank()) {
            throw violation("Password is required.");
        }
        if (password.length() < MIN_LENGTH) {
            throw violation("Password must be at least 10 characters.");
        }
        if (password.length() > MAX_LENGTH) {
            throw violation("Password is too long.");
        }
        if (normalizedEmail != null && password.equalsIgnoreCase(normalizedEmail)) {
            throw violation("Password must not match email.");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw violation("Password must contain an uppercase letter.");
        }
        if (!password.matches(".*[a-z].*")) {
            throw violation("Password must contain a lowercase letter.");
        }
        if (!password.matches(".*\\d.*")) {
            throw violation("Password must contain a digit.");
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            throw violation("Password must contain a non-alphanumeric character.");
        }
    }

    private BusinessRuleException violation(String message) {
        return new BusinessRuleException("PASSWORD_POLICY_VIOLATION", message);
    }
}
