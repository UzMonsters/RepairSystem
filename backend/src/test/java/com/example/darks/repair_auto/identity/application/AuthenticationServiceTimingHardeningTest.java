package com.example.darks.repair_auto.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.identity.infrastructure.security.JwtTokenService;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AuthenticationServiceTimingHardeningTest {

    @Test
    void givenMissingUserWhenLoginThenDummyBcryptMatchIsInvokedAndGenericErrorIsReturned() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordService passwordService = mock(PasswordService.class);
        AuthenticationService service = new AuthenticationService(
                userRepository,
                new EmailNormalizer(),
                passwordService,
                new PasswordPolicy(),
                mock(JwtTokenService.class),
                mock(RefreshSessionService.class));
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        BusinessRuleException exception = org.assertj.core.api.Assertions.catchThrowableOfType(
                () -> service.login("missing@example.com", "CandidatePass123!", null, null),
                BusinessRuleException.class);

        assertThat(exception.code()).isEqualTo("INVALID_CREDENTIALS");
        verify(passwordService).matches(eq("CandidatePass123!"), org.mockito.ArgumentMatchers.startsWith("$2a$"));
    }
}
