package com.example.darks.repair_auto.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.identity.infrastructure.persistence.RefreshSessionRepository;
import com.example.darks.repair_auto.identity.api.dto.LoginResponse;
import com.example.darks.repair_auto.identity.api.dto.TokenResponse;
import com.example.darks.repair_auto.identity.application.AuthenticationService;
import com.example.darks.repair_auto.identity.application.EmailNormalizer;
import com.example.darks.repair_auto.identity.application.PasswordService;
import com.example.darks.repair_auto.identity.application.RefreshSessionService;
import com.example.darks.repair_auto.identity.application.TokenHashService;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.identity.infrastructure.security.JwtTokenService;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AuthIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private RefreshSessionService refreshSessionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private RepairRequestRepository repairRequestRepository;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EmailNormalizer emailNormalizer;

    @Autowired
    private TokenHashService tokenHashService;

    @Autowired
    private JwtTokenService jwtTokenService;

    private User admin;
    private User manager;

    @BeforeEach
    void setUp() {
        repairRequestRepository.deleteAll();
        refreshSessionRepository.deleteAll();
        userRepository.deleteAll();
        admin = createUser("Admin User", "Admin@Example.com", "AdminPass123!", UserRole.ADMIN, true);
        manager = createUser("Manager User", "Manager@Example.com", "ManagerPass123!", UserRole.MANAGER, true);
    }

    @Test
    void givenActiveAdminWhenLoginThenTokensAreIssuedAndRefreshTokenIsStoredOnlyAsHash() {
        LoginResponse response = authenticationService.login(" ADMIN@example.COM ", "AdminPass123!", "127.0.0.1", "test");

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.user().role()).isEqualTo(UserRole.ADMIN);
        assertThat(userRepository.findById(admin.getId()).orElseThrow().getLastLoginAt()).isNotNull();
        assertThat(refreshSessionRepository.findByTokenHash(tokenHashService.hash(response.refreshToken()))).isPresent();
        assertThat(refreshSessionRepository.findAll())
                .noneMatch(session -> session.getTokenHash().equals(response.refreshToken()));
        JwtTokenService.ValidatedAccessToken token = jwtTokenService.validate(response.accessToken());
        assertThat(token.userId()).isEqualTo(admin.getId());
        assertThat(token.role()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void givenRememberMeFalseWhenLoginThenNormalTtlIsUsedAndSessionNotRemembered() {
        LoginResponse response = authenticationService.login("admin@example.com", "AdminPass123!", false, "127.0.0.1", "test");

        assertThat(response.rememberMe()).isFalse();
        var session = refreshSessionRepository.findByTokenHash(tokenHashService.hash(response.refreshToken())).orElseThrow();
        assertThat(session.isRememberMe()).isFalse();
    }

    @Test
    void givenRememberMeTrueWhenLoginThenExtendedTtlIsUsedAndSessionIsRemembered() {
        LoginResponse response = authenticationService.login("admin@example.com", "AdminPass123!", true, "127.0.0.1", "test");

        assertThat(response.rememberMe()).isTrue();
        var session = refreshSessionRepository.findByTokenHash(tokenHashService.hash(response.refreshToken())).orElseThrow();
        assertThat(session.isRememberMe()).isTrue();
        assertThat(response.refreshTokenExpiresIn()).isGreaterThan(TimeUnit.DAYS.toSeconds(20));
    }

    @Test
    void givenRememberedSessionWhenRotatedThenNewSessionInheritsRememberMeAndPreservesExpirationCap() {
        LoginResponse login = authenticationService.login("admin@example.com", "AdminPass123!", true, "127.0.0.1", "test");
        var initialSession = refreshSessionRepository.findByTokenHash(tokenHashService.hash(login.refreshToken())).orElseThrow();

        TokenResponse rotated = authenticationService.refresh(login.refreshToken(), "127.0.0.1", "test");

        assertThat(rotated.rememberMe()).isTrue();
        var rotatedSession = refreshSessionRepository.findByTokenHash(tokenHashService.hash(rotated.refreshToken())).orElseThrow();
        assertThat(rotatedSession.isRememberMe()).isTrue();
        assertThat(rotatedSession.getExpiresAt()).isEqualTo(initialSession.getExpiresAt());
    }

    @Test
    void givenActiveManagerWhenLoginThenManagerTokenIsIssued() {
        LoginResponse response = authenticationService.login("manager@example.com", "ManagerPass123!", "127.0.0.1", "test");

        assertThat(response.user().role()).isEqualTo(UserRole.MANAGER);
        assertThat(jwtTokenService.validate(response.accessToken()).role()).isEqualTo(UserRole.MANAGER);
    }

    @Test
    void givenUnknownOrWrongPasswordWhenLoginThenGenericInvalidCredentialsIsReturned() {
        assertInvalidCredentials(() -> authenticationService.login("missing@example.com", "AdminPass123!", null, null));
        assertInvalidCredentials(() -> authenticationService.login("admin@example.com", "WrongPass123!", null, null));
    }

    @Test
    void givenDisabledUserWhenLoginThenGenericInvalidCredentialsIsReturned() {
        manager.setActive(false, OffsetDateTime.now(ZoneOffset.UTC));
        userRepository.saveAndFlush(manager);

        assertInvalidCredentials(() -> authenticationService.login("manager@example.com", "ManagerPass123!", null, null));
    }

    @Test
    void givenRefreshTokenWhenRefreshThenTokenRotatesAndOldTokenReuseRevokesFamily() {
        LoginResponse login = authenticationService.login("admin@example.com", "AdminPass123!", null, null);

        TokenResponse rotated = authenticationService.refresh(login.refreshToken(), null, null);

        assertThat(rotated.refreshToken()).isNotEqualTo(login.refreshToken());
        BusinessRuleException reuse = org.assertj.core.api.Assertions.catchThrowableOfType(
                () -> authenticationService.refresh(login.refreshToken(), null, null),
                BusinessRuleException.class);
        assertThat(reuse.code()).isEqualTo("REFRESH_TOKEN_REUSE_DETECTED");
        assertThat(refreshSessionRepository.findAll())
                .allMatch(session -> session.isRevoked() || session.isUsed());
    }

    @Test
    void givenConcurrentRefreshRequestsWhenUsingSameTokenThenOnlyOneSucceeds() throws Exception {
        LoginResponse login = authenticationService.login("admin@example.com", "AdminPass123!", null, null);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Object> refreshCall = () -> {
            start.await(5, TimeUnit.SECONDS);
            try {
                return authenticationService.refresh(login.refreshToken(), null, null);
            } catch (BusinessRuleException exception) {
                return exception;
            }
        };

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(refreshCall);
            var second = executor.submit(refreshCall);
            start.countDown();
            List<Object> results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));

            assertThat(results).filteredOn(TokenResponse.class::isInstance).hasSize(1);
            assertThat(results)
                    .filteredOn(BusinessRuleException.class::isInstance)
                    .extracting(result -> ((BusinessRuleException) result).code())
                    .containsExactly("REFRESH_TOKEN_REUSE_DETECTED");
        }
    }

    @Test
    void givenLogoutWhenRepeatedThenItIsIdempotentAndTokenCannotRefresh() {
        LoginResponse login = authenticationService.login("admin@example.com", "AdminPass123!", null, null);

        authenticationService.logout(login.refreshToken());
        authenticationService.logout(login.refreshToken());

        BusinessRuleException exception = org.assertj.core.api.Assertions.catchThrowableOfType(
                () -> authenticationService.refresh(login.refreshToken(), null, null),
                BusinessRuleException.class);
        assertThat(exception.code()).isEqualTo("REFRESH_TOKEN_REVOKED");
    }

    @Test
    void givenPasswordChangeWhenSuccessfulThenSessionsAreRevokedAndOldAccessTokenIsRejected() {
        LoginResponse login = authenticationService.login("admin@example.com", "AdminPass123!", null, null);

        authenticationService.changePassword(admin.getId(), "AdminPass123!", "NewAdminPass123!", "NewAdminPass123!");

        assertThat(refreshSessionRepository.findByTokenHash(tokenHashService.hash(login.refreshToken())).orElseThrow()
                .isRevoked()).isTrue();
        assertThat(passwordService.matches("NewAdminPass123!", userRepository.findById(admin.getId()).orElseThrow()
                .getPasswordHash())).isTrue();
    }

    @Test
    void givenInvalidPasswordChangeWhenCurrentWrongOrReusedThenStableErrorsAreReturned() {
        BusinessRuleException wrong = org.assertj.core.api.Assertions.catchThrowableOfType(
                () -> authenticationService.changePassword(admin.getId(), "WrongPass123!", "NewAdminPass123!", "NewAdminPass123!"),
                BusinessRuleException.class);
        BusinessRuleException reused = org.assertj.core.api.Assertions.catchThrowableOfType(
                () -> authenticationService.changePassword(admin.getId(), "AdminPass123!", "AdminPass123!", "AdminPass123!"),
                BusinessRuleException.class);

        assertThat(wrong.code()).isEqualTo("INVALID_CURRENT_PASSWORD");
        assertThat(reused.code()).isEqualTo("PASSWORD_REUSE_NOT_ALLOWED");
    }

    private User createUser(String fullName, String email, String password, UserRole role, boolean active) {
        String normalizedEmail = emailNormalizer.normalize(email);
        User user = new User(
                fullName,
                normalizedEmail,
                passwordService.hash(password),
                role,
                active,
                OffsetDateTime.now(ZoneOffset.UTC));
        return userRepository.saveAndFlush(user);
    }

    private void assertInvalidCredentials(Runnable action) {
        BusinessRuleException exception = org.assertj.core.api.Assertions.catchThrowableOfType(
                action::run,
                BusinessRuleException.class);
        assertThat(exception.code()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(exception.status()).isEqualTo(401);
    }
}
