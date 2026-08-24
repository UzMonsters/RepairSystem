package com.example.darks.repair_auto.identity.application;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.MobileAuthProvider;
import com.example.darks.repair_auto.identity.domain.MobileSession;
import com.example.darks.repair_auto.identity.domain.MobileSessionRevocationReason;
import com.example.darks.repair_auto.identity.infrastructure.persistence.MobileSessionRepository;
import com.example.darks.repair_auto.identity.mobile.auth.dto.MobileDeviceContextRequest;
import com.example.darks.repair_auto.identity.mobile.auth.dto.MobileSessionResponse;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.notification.push.domain.PushPlatform;
import com.example.darks.repair_auto.shared.config.AppProperties;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MobileSessionService {

    private final MobileSessionRepository repository;
    private final AppProperties properties;
    private final Clock clock;

    @Autowired
    public MobileSessionService(MobileSessionRepository repository, AppProperties properties) {
        this(repository, properties, Clock.systemUTC());
    }

    MobileSessionService(MobileSessionRepository repository, AppProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public MobileSession createForCustomer(
            Customer customer,
            MobileAuthProvider provider,
            MobileDeviceContextRequest device,
            String ip,
            String userAgent) {
        OffsetDateTime now = now();
        MobileDeviceContextRequest normalized = normalizeDevice(device);
        return repository.save(MobileSession.forCustomer(
                customer,
                provider,
                normalized.platform(),
                trimToNull(normalized.deviceId()),
                trimToNull(normalized.deviceName()),
                trimToNull(normalized.appVersion()),
                trimToNull(ip),
                trimToNull(userAgent),
                now,
                now.plus(properties.mobileSessionTtl())));
    }

    @Transactional
    public MobileSession createForTechnician(
            Technician technician,
            MobileAuthProvider provider,
            MobileDeviceContextRequest device,
            String ip,
            String userAgent) {
        OffsetDateTime now = now();
        MobileDeviceContextRequest normalized = normalizeDevice(device);
        return repository.save(MobileSession.forTechnician(
                technician,
                provider,
                normalized.platform(),
                trimToNull(normalized.deviceId()),
                trimToNull(normalized.deviceName()),
                trimToNull(normalized.appVersion()),
                trimToNull(ip),
                trimToNull(userAgent),
                now,
                now.plus(properties.mobileSessionTtl())));
    }

    @Transactional
    public void requireActive(UUID sessionId, ActorType actorType, Long actorId, PushClientType clientType, String ip) {
        if (sessionId == null || actorType == null || actorId == null || clientType == null) {
            throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
        MobileSession session = repository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN));
        if (session.getActorType() != actorType
                || !session.getActorId().equals(actorId)
                || session.getClientType() != clientType
                || !session.isActiveAt(now())) {
            throw new BusinessException(ErrorCode.SESSION_REVOKED);
        }
        if (properties.mobileSessionLastSeenUpdateInterval().isZero()
                || session.getLastSeenAt().plus(properties.mobileSessionLastSeenUpdateInterval()).isBefore(now())) {
            session.touch(trimToNull(ip), now());
        }
    }

    @Transactional
    public void revoke(UUID sessionId, ActorType actorType, Long actorId, MobileSessionRevocationReason reason) {
        MobileSession session = repository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_REVOKED));
        if (session.getActorType() != actorType || !session.getActorId().equals(actorId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        session.revoke(now(), reason);
    }

    @Transactional
    public int revokeOtherSessionsForActor(
            UUID currentSessionId,
            ActorType actorType,
            Long actorId,
            MobileSessionRevocationReason reason) {
        if (currentSessionId == null) {
            return revokeAllForActor(actorType, actorId, reason);
        }
        return actorType == ActorType.CUSTOMER
                ? repository.revokeOtherSessionsForCustomer(actorId, currentSessionId, now(), reason.name())
                : repository.revokeOtherSessionsForTechnician(actorId, currentSessionId, now(), reason.name());
    }

    @Transactional
    public int revokeAllForActor(ActorType actorType, Long actorId, MobileSessionRevocationReason reason) {
        return actorType == ActorType.CUSTOMER
                ? revokeAllForCustomer(actorId, reason)
                : revokeAllForTechnician(actorId, reason);
    }

    @Transactional
    public int revokeAllForCustomer(Long customerId, MobileSessionRevocationReason reason) {
        return repository.revokeAllForCustomer(customerId, now(), reason.name());
    }

    @Transactional
    public int revokeAllForTechnician(Long technicianId, MobileSessionRevocationReason reason) {
        return repository.revokeAllForTechnician(technicianId, now(), reason.name());
    }

    @Transactional(readOnly = true)
    public List<MobileSessionResponse> list(ActorType actorType, Long actorId, PushClientType clientType) {
        return repository.findForActor(actorType, actorId, clientType).stream()
                .map(this::toResponse)
                .toList();
    }

    private MobileSessionResponse toResponse(MobileSession session) {
        return new MobileSessionResponse(
                session.getId(),
                session.getClientType(),
                session.getAuthenticationProvider(),
                session.getPlatform(),
                session.getDeviceId(),
                session.getDeviceName(),
                session.getAppVersion(),
                session.getCreatedAt(),
                session.getLastSeenAt(),
                session.getExpiresAt(),
                session.getRevokedAt() != null);
    }

    private MobileDeviceContextRequest normalizeDevice(MobileDeviceContextRequest device) {
        if (device == null || device.platform() == null) {
            return new MobileDeviceContextRequest(PushPlatform.ANDROID, null, null, null);
        }
        if (device.platform() == PushPlatform.WEB) {
            throw new BusinessException(ErrorCode.MOBILE_CLIENT_TYPE_INVALID);
        }
        return device;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
