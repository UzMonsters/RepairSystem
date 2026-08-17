package com.example.darks.repair_auto.identity.application;

import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 128;

    public void validate(String password, String normalizedEmail) {
        if (password == null || password.isBlank()) {
            throw violation();
        }
        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            throw violation();
        }
        if (normalizedEmail != null && password.equalsIgnoreCase(normalizedEmail)) {
            throw violation();
        }
        if (!password.matches(".*[A-Z].*")) {
            throw violation();
        }
        if (!password.matches(".*[a-z].*")) {
            throw violation();
        }
        if (!password.matches(".*\\d.*")) {
            throw violation();
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            throw violation();
        }
    }

    private BusinessException violation() {
        return new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION);
    }
}
