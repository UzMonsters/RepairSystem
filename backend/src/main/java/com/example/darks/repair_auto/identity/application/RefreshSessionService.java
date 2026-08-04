package com.example.darks.repair_auto.identity.application;

import com.example.darks.repair_auto.identity.domain.RefreshSession;
import com.example.darks.repair_auto.identity.infrastructure.persistence.RefreshSessionRepository;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.config.AppProperties;
import com.example.darks.repair_auto.identity.domain.User;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshSessionService {

    private final RefreshSessionRepository repository;
    private final RefreshTokenGenerator tokenGenerator;
    private final TokenHashService tokenHashService;
    private final AppProperties properties;
    private final Clock clock;

    public RefreshSessionService(
            RefreshSessionRepository repository,
            RefreshTokenGenerator tokenGenerator,
            TokenHashService tokenHashService,
            AppProperties properties) {
        this.repository = repository;
        this.tokenGenerator = tokenGenerator;
        this.tokenHashService = tokenHashService;
        this.properties = properties;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public IssuedRefreshToken create(User user, String ip, String userAgent) {
        return createInFamily(user, UUID.randomUUID(), ip, userAgent);
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public RotationResult rotate(String rawToken, String ip, String userAgent) {
        OffsetDateTime now = now();
        RefreshSession session = repository.findByTokenHashForUpdate(tokenHashService.hash(rawToken))
                .orElseThrow(() -> new BusinessRuleException("INVALID_REFRESH_TOKEN", "Refresh token is invalid.", 401));
        if (session.isUsed()) {
            repository.revokeFamily(session.getTokenFamilyId(), now, "REUSE_DETECTED");
            throw new BusinessRuleException(
                    "REFRESH_TOKEN_REUSE_DETECTED",
                    "Refresh token reuse was detected.",
                    401);
        }
        if (session.isRevoked()) {
            throw new BusinessRuleException("REFRESH_TOKEN_REVOKED", "Refresh token has been revoked.", 401);
        }
        if (session.isExpired(now)) {
            throw new BusinessRuleException("REFRESH_TOKEN_EXPIRED", "Refresh token has expired.", 401);
        }
        if (!session.getUser().isActive()) {
            throw new BusinessRuleException("USER_DISABLED", "User account is disabled.", 401);
        }
        session.markUsed(now, ip, userAgent);
        IssuedRefreshToken replacement = createInFamily(session.getUser(), session.getTokenFamilyId(), ip, userAgent);
        session.replaceWith(replacement.session().getId());
        return new RotationResult(session.getUser(), replacement.rawToken(), replacement.session());
    }

    @Transactional
    public void revokeByRawToken(String rawToken, String reason) {
        repository.findByTokenHashForUpdate(tokenHashService.hash(rawToken))
                .ifPresent(session -> session.revoke(now(), reason));
    }

    @Transactional
    public void revokeAllForUser(Long userId, String reason) {
        repository.revokeAllForUser(userId, now(), reason);
    }

    public long refreshTokenTtlSeconds() {
        return properties.refreshTokenTtl().toSeconds();
    }

    private IssuedRefreshToken createInFamily(User user, UUID familyId, String ip, String userAgent) {
        OffsetDateTime now = now();
        String rawToken = tokenGenerator.generate();
        RefreshSession session = new RefreshSession(
                user,
                tokenHashService.hash(rawToken),
                familyId,
                now,
                now.plus(properties.refreshTokenTtl()),
                ip,
                userAgent);
        RefreshSession saved = repository.saveAndFlush(session);
        return new IssuedRefreshToken(rawToken, saved);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }

    public record IssuedRefreshToken(String rawToken, RefreshSession session) {
    }

    public record RotationResult(User user, String rawToken, RefreshSession session) {
    }
}
