package com.example.darks.repair_auto.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.config.AppProperties;
import com.example.darks.repair_auto.user.domain.User;
import com.example.darks.repair_auto.user.domain.UserRepository;
import com.example.darks.repair_auto.user.domain.UserRole;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BootstrapAdminRunnerTest {

    private final UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
    private final BootstrapAdminRunner runner = new BootstrapAdminRunner(
            properties(true, " Admin@Example.com ", "BootstrapPass123!", " Root Admin "),
            userRepository,
            new EmailNormalizer(),
            new PasswordPolicy(),
            new PasswordService());

    @Test
    void givenNoAdminAndBootstrapEnabledWhenRunThenAdminIsCreated() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);

        runner.run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getFullName()).isEqualTo("Root Admin");
        assertThat(saved.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getPasswordHash()).startsWith("$2");
    }

    @Test
    void givenExistingAdminWhenRunThenNoUserIsCreated() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(true);

        runner.run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void givenMissingCredentialsWhenRunThenStartupFails() {
        BootstrapAdminRunner missingCredentials = new BootstrapAdminRunner(
                properties(true, "", "", "Root Admin"),
                userRepository,
                new EmailNormalizer(),
                new PasswordPolicy(),
                new PasswordService());
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> missingCredentials.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_BOOTSTRAP_ADMIN_EMAIL");
    }

    private AppProperties properties(boolean enabled, String email, String password, String fullName) {
        return new AppProperties(
                new AppProperties.Cors(List.of(), List.of(), List.of(), List.of(), false),
                new AppProperties.Trace("X-Trace-Id"),
                new AppProperties.Jwt(
                        "test-local-only-jwt-secret-that-is-long-enough",
                        "repair-auto",
                        Duration.ofMinutes(15)),
                Duration.ofDays(30),
                new AppProperties.BootstrapAdmin(enabled, email, password, fullName));
    }
}
