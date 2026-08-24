package com.example.darks.repair_auto.identity.application;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.MobileSession;
import com.example.darks.repair_auto.identity.domain.MobileSessionRevocationReason;
import com.example.darks.repair_auto.identity.domain.MobileRefreshRevocationReason;
import com.example.darks.repair_auto.identity.domain.MobileRefreshSession;
import com.example.darks.repair_auto.identity.infrastructure.persistence.MobileRefreshSessionRepository;
import com.example.darks.repair_auto.notification.push.application.PushEndpointService;
import com.example.darks.repair_auto.shared.config.AppProperties;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MobileRefreshSessionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MobileRefreshSessionService.class);

    private final MobileRefreshSessionRepository repository;
    private final CustomerRepository customerRepository;
    private final TechnicianRepository technicianRepository;
    private final RefreshTokenGenerator tokenGenerator;
    private final TokenHashService tokenHashService;
    private final AppProperties properties;
    private final PushEndpointService pushEndpointService;
    private final MobileSessionService mobileSessionService;
    private final Clock clock;

    @Autowired
    public MobileRefreshSessionService(
            MobileRefreshSessionRepository repository,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            RefreshTokenGenerator tokenGenerator,
            TokenHashService tokenHashService,
            AppProperties properties,
            PushEndpointService pushEndpointService,
            @Autowired(required = false) MobileSessionService mobileSessionService) {
        this(repository, customerRepository, technicianRepository, tokenGenerator, tokenHashService, properties, pushEndpointService,
                mobileSessionService, Clock.systemUTC());
    }

    public MobileRefreshSessionService(
            MobileRefreshSessionRepository repository,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            RefreshTokenGenerator tokenGenerator,
            TokenHashService tokenHashService,
            AppProperties properties) {
        this(repository, customerRepository, technicianRepository, tokenGenerator, tokenHashService, properties, null, null, Clock.systemUTC());
    }

    MobileRefreshSessionService(
            MobileRefreshSessionRepository repository,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            RefreshTokenGenerator tokenGenerator,
            TokenHashService tokenHashService,
            AppProperties properties,
            PushEndpointService pushEndpointService,
            MobileSessionService mobileSessionService,
            Clock clock) {
        this.repository = repository;
        this.customerRepository = customerRepository;
        this.technicianRepository = technicianRepository;
        this.tokenGenerator = tokenGenerator;
        this.tokenHashService = tokenHashService;
        this.properties = properties;
        this.pushEndpointService = pushEndpointService;
        this.mobileSessionService = mobileSessionService;
        this.clock = clock;
    }

    MobileRefreshSessionService(
            MobileRefreshSessionRepository repository,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            RefreshTokenGenerator tokenGenerator,
            TokenHashService tokenHashService,
            AppProperties properties,
            PushEndpointService pushEndpointService,
            Clock clock) {
        this(repository, customerRepository, technicianRepository, tokenGenerator, tokenHashService,
                properties, pushEndpointService, null, clock);
    }

    @Transactional
    public IssuedMobileRefreshToken createForCustomer(Customer customer) {
        return createForCustomer(customer, null);
    }

    @Transactional
    public IssuedMobileRefreshToken createForCustomer(Customer customer, MobileSession mobileSession) {
        OffsetDateTime now = now();
        String rawToken = tokenGenerator.generate();
        String tokenHash = tokenHashService.hash(rawToken);
        UUID tokenFamilyId = UUID.randomUUID();
        OffsetDateTime expiresAt = now.plus(properties.mobileRefreshTokenTtl());

        MobileRefreshSession session = MobileRefreshSession.forCustomer(
                customer,
                tokenHash,
                tokenFamilyId,
                null,
                mobileSession,
                now,
                expiresAt);

        MobileRefreshSession saved = repository.save(session);
        LOGGER.info("Created mobile refresh session for customerId={}, familyId={}", customer.getId(), tokenFamilyId);
        return new IssuedMobileRefreshToken(rawToken, saved);
    }

    @Transactional
    public IssuedMobileRefreshToken createForTechnician(Technician technician) {
        return createForTechnician(technician, null);
    }

    @Transactional
    public IssuedMobileRefreshToken createForTechnician(Technician technician, MobileSession mobileSession) {
        OffsetDateTime now = now();
        String rawToken = tokenGenerator.generate();
        String tokenHash = tokenHashService.hash(rawToken);
        UUID tokenFamilyId = UUID.randomUUID();
        OffsetDateTime expiresAt = now.plus(properties.mobileRefreshTokenTtl());

        MobileRefreshSession session = MobileRefreshSession.forTechnician(
                technician,
                tokenHash,
                tokenFamilyId,
                null,
                mobileSession,
                now,
                expiresAt);

        MobileRefreshSession saved = repository.save(session);
        LOGGER.info("Created mobile refresh session for technicianId={}, familyId={}", technician.getId(), tokenFamilyId);
        return new IssuedMobileRefreshToken(rawToken, saved);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public MobileRotationResult rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException(ErrorCode.MOBILE_REFRESH_TOKEN_INVALID);
        }

        OffsetDateTime now = now();
        String tokenHash = tokenHashService.hash(rawToken);

        MobileRefreshSession session = repository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOBILE_REFRESH_TOKEN_INVALID));

        if (session.isUsed()) {
            LOGGER.warn("Mobile refresh token reuse detected for familyId={}, revoking all sessions and endpoints for actorType={}",
                    session.getTokenFamilyId(), session.getActorType());
            if (session.getActorType() == ActorType.CUSTOMER && session.getCustomer() != null) {
                repository.revokeAllForCustomer(session.getCustomer().getId(), now, MobileRefreshRevocationReason.REUSE_DETECTED.name());
                revokeMobileSessionsForCustomer(session.getCustomer().getId(), MobileSessionRevocationReason.REFRESH_REUSE_DETECTED);
                if (pushEndpointService != null) {
                    pushEndpointService.disableAllForCustomer(session.getCustomer().getId());
                }
            } else if (session.getActorType() == ActorType.TECHNICIAN && session.getTechnician() != null) {
                repository.revokeAllForTechnician(session.getTechnician().getId(), now, MobileRefreshRevocationReason.REUSE_DETECTED.name());
                revokeMobileSessionsForTechnician(session.getTechnician().getId(), MobileSessionRevocationReason.REFRESH_REUSE_DETECTED);
                if (pushEndpointService != null) {
                    pushEndpointService.disableAllForTechnician(session.getTechnician().getId());
                }
            } else {
                repository.revokeFamily(session.getTokenFamilyId(), now, MobileRefreshRevocationReason.REUSE_DETECTED.name());
            }
            throw new BusinessException(ErrorCode.MOBILE_REFRESH_TOKEN_REUSED);
        }

        if (session.isRevoked()) {
            if (MobileRefreshRevocationReason.REUSE_DETECTED.name().equals(session.getRevocationReason())) {
                LOGGER.warn("Attempt to use revoked session in reused familyId={}", session.getTokenFamilyId());
                throw new BusinessException(ErrorCode.MOBILE_REFRESH_TOKEN_REUSED);
            }
            throw new BusinessException(ErrorCode.MOBILE_REFRESH_TOKEN_INVALID);
        }

        if (session.isExpired(now)) {
            throw new BusinessException(ErrorCode.MOBILE_REFRESH_TOKEN_EXPIRED);
        }

        if (session.getMobileSession() == null) {
            LOGGER.warn("Legacy session-less mobile refresh token rejected for familyId={}", session.getTokenFamilyId());
            throw new BusinessException(ErrorCode.MOBILE_REFRESH_TOKEN_EXPIRED);
        }

        if (!session.getMobileSession().isActiveAt(now)) {
            LOGGER.warn("Mobile refresh token rejected because associated mobile session is inactive for familyId={}",
                    session.getTokenFamilyId());
            throw new BusinessException(ErrorCode.SESSION_REVOKED);
        }

        Customer customer = null;
        Technician technician = null;

        if (session.getActorType() == ActorType.CUSTOMER) {
            customer = customerRepository.findById(session.getCustomer().getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE));
            if (!customer.isActive()) {
                LOGGER.info("Customer is inactive during mobile refresh. Revoking session family.");
                repository.revokeFamily(session.getTokenFamilyId(), now, MobileRefreshRevocationReason.ACCOUNT_INACTIVE.name());
                revokeSession(session, MobileSessionRevocationReason.ACCOUNT_INACTIVE);
                throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
            }
        } else if (session.getActorType() == ActorType.TECHNICIAN) {
            technician = technicianRepository.findById(session.getTechnician().getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE));
            if (!technician.isActive()) {
                LOGGER.info("Technician is inactive during mobile refresh. Revoking session family.");
                repository.revokeFamily(session.getTokenFamilyId(), now, MobileRefreshRevocationReason.ACCOUNT_INACTIVE.name());
                revokeSession(session, MobileSessionRevocationReason.ACCOUNT_INACTIVE);
                throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
            }
        } else {
            throw new BusinessException(ErrorCode.MOBILE_REFRESH_TOKEN_INVALID);
        }

        session.markUsed(now);
        session.revoke(now, MobileRefreshRevocationReason.ROTATED.name());

        String newRawToken = tokenGenerator.generate();
        String newTokenHash = tokenHashService.hash(newRawToken);
        OffsetDateTime newExpiresAt = now.plus(properties.mobileRefreshTokenTtl());

        MobileRefreshSession replacementSession;
        if (session.getActorType() == ActorType.CUSTOMER) {
            replacementSession = MobileRefreshSession.forCustomer(
                    customer,
                    newTokenHash,
                    session.getTokenFamilyId(),
                    session.getId(),
                    session.getMobileSession(),
                    now,
                    newExpiresAt);
        } else {
            replacementSession = MobileRefreshSession.forTechnician(
                    technician,
                    newTokenHash,
                    session.getTokenFamilyId(),
                    session.getId(),
                    session.getMobileSession(),
                    now,
                    newExpiresAt);
        }

        MobileRefreshSession savedReplacement = repository.save(replacementSession);
        session.replaceWith(savedReplacement.getId(), now);

        LOGGER.info("Rotated mobile refresh session for actorType={}, actorId={}, familyId={}",
                session.getActorType(), session.getActorId(), session.getTokenFamilyId());

        return new MobileRotationResult(
                session.getActorType(),
                customer,
                technician,
                newRawToken,
                savedReplacement);
    }

    @Transactional
    public void revokeByRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String tokenHash = tokenHashService.hash(rawToken);
        repository.findByTokenHashForUpdate(tokenHash)
                .ifPresent(session -> {
                    LOGGER.info("Revoking mobile refresh family for familyId={}", session.getTokenFamilyId());
                    repository.revokeFamily(session.getTokenFamilyId(), now(), MobileRefreshRevocationReason.LOGOUT.name());
                    revokeSession(session, MobileSessionRevocationReason.LOGOUT);
                });
    }

    @Transactional
    public void revokeOtherFamiliesForActor(
            UUID currentSessionId,
            ActorType actorType,
            Long actorId,
            MobileRefreshRevocationReason reason) {
        if (currentSessionId == null) {
            if (actorType == ActorType.CUSTOMER) {
                revokeAllForCustomer(actorId, reason);
            } else {
                revokeAllForTechnician(actorId, reason);
            }
            return;
        }
        OffsetDateTime now = now();
        if (actorType == ActorType.CUSTOMER) {
            repository.revokeOtherSessionsForCustomer(actorId, currentSessionId, now, reason.name());
        } else {
            repository.revokeOtherSessionsForTechnician(actorId, currentSessionId, now, reason.name());
        }
        if (mobileSessionService != null) {
            mobileSessionService.revokeOtherSessionsForActor(currentSessionId, actorType, actorId, toSessionReason(reason));
        }
    }

    @Transactional
    public void revokeAllForCustomer(Long customerId, MobileRefreshRevocationReason reason) {
        LOGGER.info("Revoking all mobile refresh sessions for customerId={}, reason={}", customerId, reason);
        repository.revokeAllForCustomer(customerId, now(), reason.name());
        revokeMobileSessionsForCustomer(customerId, toSessionReason(reason));
    }

    @Transactional
    public void revokeAllForTechnician(Long technicianId, MobileRefreshRevocationReason reason) {
        LOGGER.info("Revoking all mobile refresh sessions for technicianId={}, reason={}", technicianId, reason);
        repository.revokeAllForTechnician(technicianId, now(), reason.name());
        revokeMobileSessionsForTechnician(technicianId, toSessionReason(reason));
    }

    public long remainingTtlSeconds(MobileRefreshSession session) {
        if (session == null || session.getExpiresAt() == null) {
            return 0;
        }
        long seconds = Duration.between(now(), session.getExpiresAt()).toSeconds();
        return Math.max(0, seconds);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private void revokeSession(MobileRefreshSession session, MobileSessionRevocationReason reason) {
        if (mobileSessionService != null && session.getMobileSession() != null) {
            mobileSessionService.revoke(
                    session.getMobileSession().getId(),
                    session.getActorType(),
                    session.getActorId(),
                    reason);
        }
    }

    private void revokeMobileSessionsForCustomer(Long customerId, MobileSessionRevocationReason reason) {
        if (mobileSessionService != null) {
            mobileSessionService.revokeAllForCustomer(customerId, reason);
        }
    }

    private void revokeMobileSessionsForTechnician(Long technicianId, MobileSessionRevocationReason reason) {
        if (mobileSessionService != null) {
            mobileSessionService.revokeAllForTechnician(technicianId, reason);
        }
    }

    private MobileSessionRevocationReason toSessionReason(MobileRefreshRevocationReason reason) {
        return switch (reason) {
            case LOGOUT -> MobileSessionRevocationReason.LOGOUT;
            case LOGOUT_ALL -> MobileSessionRevocationReason.LOGOUT_ALL;
            case REUSE_DETECTED -> MobileSessionRevocationReason.REFRESH_REUSE_DETECTED;
            case ACCOUNT_INACTIVE -> MobileSessionRevocationReason.ACCOUNT_INACTIVE;
            case TELEGRAM_IDENTITY_CHANGED, IDENTITY_CHANGED -> MobileSessionRevocationReason.IDENTITY_CHANGED;
            case CREDENTIAL_CHANGED -> MobileSessionRevocationReason.CREDENTIAL_CHANGED;
            case AUTH_METHOD_UNLINKED -> MobileSessionRevocationReason.AUTH_METHOD_UNLINKED;
            default -> MobileSessionRevocationReason.ADMIN_REVOKED;
        };
    }

    public record IssuedMobileRefreshToken(String rawToken, MobileRefreshSession session) {
    }

    public record MobileRotationResult(
            ActorType actorType,
            Customer customer,
            Technician technician,
            String rawRefreshToken,
            MobileRefreshSession session
    ) {
        public Long actorId() {
            return actorType == ActorType.CUSTOMER ? customer.getId() : technician.getId();
        }

        public String phone() {
            return actorType == ActorType.CUSTOMER ? customer.getPhone() : technician.getPhone();
        }

        public String fullName() {
            return actorType == ActorType.CUSTOMER ? customer.getFullName() : technician.getFullName();
        }
    }
}
