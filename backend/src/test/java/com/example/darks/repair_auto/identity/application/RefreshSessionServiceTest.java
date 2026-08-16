package com.example.darks.repair_auto.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.identity.domain.RefreshSession;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.persistence.RefreshSessionRepository;
import com.example.darks.repair_auto.shared.config.AppProperties;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
