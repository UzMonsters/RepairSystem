package com.example.darks.repair_auto.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.identity.domain.RefreshSession;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.persistence.RefreshSessionRepository;
import com.example.darks.repair_auto.notification.push.application.PushEndpointService;
import com.example.darks.repair_auto.shared.config.AppProperties;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RefreshSessionServiceTest {

    private RefreshSessionRepository repository;
    private RefreshTokenGenerator tokenGenerator;
    private TokenHashService tokenHashService;
    private AppProperties properties;
    private RefreshSessionService refreshSessionService;
    private User testUser;

    @BeforeEach
    void setUp() {
        repository = mock(RefreshSessionRepository.class);
        tokenGenerator = mock(RefreshTokenGenerator.class);
        tokenHashService = new TokenHashService();
        properties = new AppProperties(
                null,
                null,
                new AppProperties.Jwt("secret", "repair-auto", Duration.ofMinutes(15)),
                Duration.ofDays(1),
                Duration.ofDays(30),
                null);
        refreshSessionService = new RefreshSessionService(repository, tokenGenerator, tokenHashService, properties);
        when(tokenGenerator.generate()).thenReturn("raw-refresh-token");
        when(repository.saveAndFlush(any(RefreshSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        testUser = new User("Test User", "test@example.com", "hash", UserRole.MANAGER, true, OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(testUser, "id", 55L);
    }

    @Test
    void givenRememberMeTrueWhenCreatedThenSessionHasExtendedTtlAndRememberMeTrue() {
        RefreshSessionService.IssuedRefreshToken result = refreshSessionService.create(testUser, true, "127.0.0.1", "Agent");

        assertThat(result.session().isRememberMe()).isTrue();
        assertThat(result.session().getExpiresAt()).isAfter(OffsetDateTime.now(ZoneOffset.UTC).plusDays(29));
    }

    @Test
    void givenRememberMeFalseWhenCreatedThenSessionHasStandardTtlAndRememberMeFalse() {
        RefreshSessionService.IssuedRefreshToken result = refreshSessionService.create(testUser, false, "127.0.0.1", "Agent");

        assertThat(result.session().isRememberMe()).isFalse();
        assertThat(result.session().getExpiresAt()).isBefore(OffsetDateTime.now(ZoneOffset.UTC).plusDays(2));
    }

    @Test
    void givenRememberedSessionWhenRotatedThenNewSessionInheritsRememberMeAndPreservesExpirationCap() {
        OffsetDateTime initialExpiry = OffsetDateTime.now(ZoneOffset.UTC).plusDays(25);
        RefreshSession existingSession = new RefreshSession(
                testUser,
                tokenHashService.hash("old-token"),
                UUID.randomUUID(),
                true,
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(5),
                initialExpiry,
                "127.0.0.1",
                "Agent");

        when(repository.findByTokenHashForUpdate(tokenHashService.hash("old-token"))).thenReturn(Optional.of(existingSession));
        when(tokenGenerator.generate()).thenReturn("new-token");

        RefreshSessionService.RotationResult rotation = refreshSessionService.rotate("old-token", "127.0.0.1", "Agent");

        assertThat(rotation.session().isRememberMe()).isTrue();
        assertThat(rotation.session().getExpiresAt()).isEqualTo(initialExpiry);
    }

    @Test
    void givenFreshTokenWhenRotatedThenOldSessionRecordsReplacement() {
        RefreshSession existingSession = new RefreshSession(
                testUser,
                tokenHashService.hash("old-token"),
                UUID.randomUUID(),
                false,
                OffsetDateTime.now(ZoneOffset.UTC).minusHours(1),
                OffsetDateTime.now(ZoneOffset.UTC).plusDays(1),
                "127.0.0.1",
                "Agent");
        ReflectionTestUtils.setField(existingSession, "id", 10L);

        when(repository.findByTokenHashForUpdate(tokenHashService.hash("old-token"))).thenReturn(Optional.of(existingSession));
        when(tokenGenerator.generate()).thenReturn("new-token");
        when(repository.saveAndFlush(any(RefreshSession.class))).thenAnswer(invocation -> {
            RefreshSession saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 99L);
            return saved;
        });

        RefreshSessionService.RotationResult rotation = refreshSessionService.rotate("old-token", "127.0.0.1", "Agent");

        assertThat(rotation.session().getId()).isEqualTo(99L);
        assertThat(existingSession.isUsed()).isTrue();
        assertThat(ReflectionTestUtils.getField(existingSession, "replacedByTokenId")).isEqualTo(99L);
    }

    @Test
    void givenUsedTokenWhenRotatedThenFamilyAndStaffPushEndpointsAreRevoked() {
        PushEndpointService pushEndpointService = mock(PushEndpointService.class);
        RefreshSessionService serviceWithPush = new RefreshSessionService(
                repository,
                tokenGenerator,
                tokenHashService,
                properties,
                pushEndpointService);
        RefreshSession usedSession = new RefreshSession(
                testUser,
                tokenHashService.hash("used-token"),
                UUID.randomUUID(),
                false,
                OffsetDateTime.now(ZoneOffset.UTC).minusHours(1),
                OffsetDateTime.now(ZoneOffset.UTC).plusDays(1),
                "127.0.0.1",
                "Agent");
        usedSession.markUsed(OffsetDateTime.now(ZoneOffset.UTC), "127.0.0.1", "Agent");
        when(repository.findByTokenHashForUpdate(tokenHashService.hash("used-token"))).thenReturn(Optional.of(usedSession));

        assertThatThrownBy(() -> serviceWithPush.rotate("used-token", "127.0.0.1", "Agent"))
                .isInstanceOf(BusinessRuleException.class)
                .matches(e -> ((BusinessRuleException) e).code().equals("REFRESH_TOKEN_REUSE_DETECTED"));
        verify(repository).revokeAllForUser(eq(55L), any(), eq("REUSE_DETECTED"));
        verify(pushEndpointService).disableAllForStaff(55L);
    }
}
