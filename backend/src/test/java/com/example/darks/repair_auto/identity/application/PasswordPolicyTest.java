package com.example.darks.repair_auto.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.example.darks.repair_auto.shared.error.BusinessException;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    private final PasswordPolicy passwordPolicy = new PasswordPolicy();

    @Test
    void givenComplexPasswordWhenValidatedThenItIsAccepted() {
        assertThatCode(() -> passwordPolicy.validate("StrongPass123!", "user@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void givenWeakPasswordWhenValidatedThenStableErrorCodeIsReturned() {
        BusinessException exception = catchThrowableOfType(
                () -> passwordPolicy.validate("password", "user@example.com"),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("PASSWORD_POLICY_VIOLATION");
    }

    @Test
    void givenPasswordEqualToEmailWhenValidatedThenStableErrorCodeIsReturned() {
        BusinessException exception = catchThrowableOfType(
                () -> passwordPolicy.validate("User1@Example.com", "user1@example.com"),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("PASSWORD_POLICY_VIOLATION");
    }
}
