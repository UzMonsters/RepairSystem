package com.example.darks.repair_auto.identity.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.example.darks.repair_auto.shared.error.BusinessRuleException;
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
        BusinessRuleException exception = catchThrowableOfType(
                () -> passwordPolicy.validate("password", "user@example.com"),
                BusinessRuleException.class);

        org.assertj.core.api.Assertions.assertThat(exception.code()).isEqualTo("PASSWORD_POLICY_VIOLATION");
    }

    @Test
    void givenPasswordEqualToEmailWhenValidatedThenStableErrorCodeIsReturned() {
        BusinessRuleException exception = catchThrowableOfType(
                () -> passwordPolicy.validate("User1@Example.com", "user1@example.com"),
                BusinessRuleException.class);

        org.assertj.core.api.Assertions.assertThat(exception.code()).isEqualTo("PASSWORD_POLICY_VIOLATION");
    }
}
