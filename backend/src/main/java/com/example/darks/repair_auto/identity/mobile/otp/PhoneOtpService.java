package com.example.darks.repair_auto.identity.mobile.otp;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.application.MobileRefreshSessionService;
import com.example.darks.repair_auto.identity.application.MobileSessionService;
import com.example.darks.repair_auto.identity.application.TokenHashService;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.MobileAuthIdentity;
import com.example.darks.repair_auto.identity.domain.MobileAuthProvider;
import com.example.darks.repair_auto.identity.domain.MobileRefreshRevocationReason;
import com.example.darks.repair_auto.identity.domain.PhoneOtpChallenge;
import com.example.darks.repair_auto.identity.domain.PhoneOtpPurpose;
import com.example.darks.repair_auto.identity.infrastructure.persistence.MobileAuthIdentityRepository;
import com.example.darks.repair_auto.identity.infrastructure.persistence.PhoneOtpChallengeRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.mobile.auth.MobileAuthenticationService;
import com.example.darks.repair_auto.identity.mobile.auth.MobileClientTypeResolver;
import com.example.darks.repair_auto.identity.mobile.auth.VerifiedMobileIdentity;
import com.example.darks.repair_auto.identity.mobile.auth.dto.PhoneOtpRequest;
import com.example.darks.repair_auto.identity.mobile.auth.dto.PhoneOtpResponse;
import com.example.darks.repair_auto.identity.mobile.auth.dto.PhoneOtpVerifyRequest;
import com.example.darks.repair_auto.identity.mobile.auth.dto.PhoneVerificationConfirmRequest;
import com.example.darks.repair_auto.identity.mobile.auth.dto.PhoneVerificationRequest;
import com.example.darks.repair_auto.identity.mobile.telegram.dto.MobileAuthResponse;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.shared.phone.PhoneNumberNormalizer;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PhoneOtpService {

    private final PhoneOtpChallengeRepository repository;
    private final PhoneNumberNormalizer phoneNumberNormalizer;
    private final TokenHashService tokenHashService;
    private final PhoneOtpProperties properties;
    private final MobileAuthenticationService authenticationService;
    private final SmsSender smsSender;
    private final CustomerRepository customerRepository;
    private final TechnicianRepository technicianRepository;
    private final MobileAuthIdentityRepository identityRepository;
    private final MobileSessionService mobileSessionService;
    private final MobileRefreshSessionService refreshSessionService;
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    @Autowired
    public PhoneOtpService(
            PhoneOtpChallengeRepository repository,
            PhoneNumberNormalizer phoneNumberNormalizer,
            TokenHashService tokenHashService,
            PhoneOtpProperties properties,
            MobileAuthenticationService authenticationService,
            SmsSender smsSender,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            MobileAuthIdentityRepository identityRepository,
            MobileSessionService mobileSessionService,
            MobileRefreshSessionService refreshSessionService) {
        this(repository, phoneNumberNormalizer, tokenHashService, properties, authenticationService, smsSender,
                customerRepository, technicianRepository, identityRepository, mobileSessionService, refreshSessionService, Clock.systemUTC());
    }

    PhoneOtpService(
            PhoneOtpChallengeRepository repository,
            PhoneNumberNormalizer phoneNumberNormalizer,
            TokenHashService tokenHashService,
            PhoneOtpProperties properties,
            MobileAuthenticationService authenticationService,
            SmsSender smsSender,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            MobileAuthIdentityRepository identityRepository,
            MobileSessionService mobileSessionService,
            MobileRefreshSessionService refreshSessionService,
            Clock clock) {
        this.repository = repository;
        this.phoneNumberNormalizer = phoneNumberNormalizer;
        this.tokenHashService = tokenHashService;
        this.properties = properties;
        this.authenticationService = authenticationService;
        this.smsSender = smsSender;
        this.customerRepository = customerRepository;
        this.technicianRepository = technicianRepository;
        this.identityRepository = identityRepository;
        this.mobileSessionService = mobileSessionService;
        this.refreshSessionService = refreshSessionService;
        this.clock = clock;
    }

    @Transactional
    public PhoneOtpResponse request(PhoneOtpRequest request, String ip, String userAgent) {
        String phone = phoneNumberNormalizer.normalize(request.phone());
        OffsetDateTime now = now();
        repository.findTopByPhoneAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(phone, now)
                .filter(open -> open.getClientType() == request.clientType())
                .ifPresent(open -> {
                    if (open.getResendAvailableAt().isAfter(now)) {
                        throw new BusinessException(ErrorCode.PHONE_OTP_RESEND_COOLDOWN);
                    }
                });
        ActorType actorType = MobileClientTypeResolver.actorType(request.clientType());
        String code = "%06d".formatted(random.nextInt(1_000_000));
        PhoneOtpChallenge challenge = repository.save(new PhoneOtpChallenge(
                phone,
                actorType,
                request.clientType(),
                actorType == ActorType.CUSTOMER
                        ? PhoneOtpPurpose.CUSTOMER_REGISTER_OR_LOGIN
                        : PhoneOtpPurpose.TECHNICIAN_LOGIN,
                tokenHashService.hash(code),
                properties.getMaxAttempts(),
                now,
                now.plus(properties.getTtl()),
                now.plus(properties.getResendCooldown()),
                ip,
                userAgent));

        String deliveryStatus = "SMS_DISABLED";
        if (smsSender != null && smsSender.isEnabled()) {
            try {
                smsSender.sendOtp(phone, code, LanguageCode.UZ);
                deliveryStatus = "SENT";
            } catch (Exception exception) {
                challenge.consume(now);
                repository.save(challenge);
                if (exception instanceof BusinessException be) {
                    throw be;
                }
                throw new BusinessException(ErrorCode.SMS_DELIVERY_FAILED);
            }
        }

        return new PhoneOtpResponse(
                challenge.getId(),
                properties.getTtl().toSeconds(),
                properties.getResendCooldown().toSeconds(),
                deliveryStatus,
                properties.isExposeCodeInResponse() ? code : null);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public MobileAuthResponse verify(PhoneOtpVerifyRequest request, String ip, String userAgent) {
        PhoneOtpChallenge challenge = repository.findByIdForUpdate(request.challengeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_OTP_INVALID));
        OffsetDateTime now = now();
        if (challenge.isConsumed()) {
            throw new BusinessException(ErrorCode.PHONE_OTP_INVALID);
        }
        if (challenge.isExpired(now)) {
            throw new BusinessException(ErrorCode.PHONE_OTP_EXPIRED);
        }
        if (!challenge.hasAttemptsRemaining()) {
            throw new BusinessException(ErrorCode.PHONE_OTP_ATTEMPTS_EXCEEDED);
        }
        if (!tokenHashService.hash(request.code()).equals(challenge.getCodeHash())) {
            challenge.failAttempt();
            throw new BusinessException(ErrorCode.PHONE_OTP_INVALID);
        }
        challenge.consume(now);
        return authenticationService.authenticate(
                challenge.getClientType(),
                new VerifiedMobileIdentity(
                        MobileAuthProvider.PHONE,
                        challenge.getPhone(),
                        null,
                        false,
                        challenge.getPhone(),
                        challenge.getPhone()),
                request.device(),
                ip,
                userAgent);
    }

    @Transactional
    public PhoneOtpResponse requestVerificationForActor(
            AuthenticatedMobileActor actor,
            PhoneVerificationRequest request,
            String ip,
            String userAgent) {
        requireActor(actor);
        String phone = phoneNumberNormalizer.normalize(request.phone());
        OffsetDateTime now = now();

        PushClientType clientType = MobileClientTypeResolver.clientType(actor.actorType());

        repository.findTopByPhoneAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(phone, now)
                .filter(open -> open.getClientType() == clientType)
                .ifPresent(open -> {
                    if (open.getResendAvailableAt().isAfter(now)) {
                        throw new BusinessException(ErrorCode.PHONE_OTP_RESEND_COOLDOWN);
                    }
                });

        LanguageCode preferredLanguage = LanguageCode.UZ;
        if (actor.actorType() == ActorType.CUSTOMER) {
            Customer customer = customerRepository.findById(actor.actorId()).orElse(null);
            if (customer != null && customer.getPreferredLanguage() != null) {
                preferredLanguage = customer.getPreferredLanguage();
            }
        } else if (actor.actorType() == ActorType.TECHNICIAN) {
            Technician technician = technicianRepository.findById(actor.actorId()).orElse(null);
            if (technician != null && technician.getPreferredLanguage() != null) {
                preferredLanguage = technician.getPreferredLanguage();
            }
        }

        String code = "%06d".formatted(random.nextInt(1_000_000));
        PhoneOtpChallenge challenge = repository.save(new PhoneOtpChallenge(
                phone,
                actor.actorType(),
                clientType,
                PhoneOtpPurpose.CHANGE_PHONE,
                tokenHashService.hash(code),
                properties.getMaxAttempts(),
                now,
                now.plus(properties.getTtl()),
                now.plus(properties.getResendCooldown()),
                ip,
                userAgent));

        String deliveryStatus = "SMS_DISABLED";
        if (smsSender != null && smsSender.isEnabled()) {
            try {
                smsSender.sendOtp(phone, code, preferredLanguage);
                deliveryStatus = "SENT";
            } catch (Exception exception) {
                challenge.consume(now);
                repository.save(challenge);
                if (exception instanceof BusinessException be) {
                    throw be;
                }
                throw new BusinessException(ErrorCode.SMS_DELIVERY_FAILED);
            }
        }

        return new PhoneOtpResponse(
                challenge.getId(),
                properties.getTtl().toSeconds(),
                properties.getResendCooldown().toSeconds(),
                deliveryStatus,
                properties.isExposeCodeInResponse() ? code : null);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public void verifyAndChangePhone(
            AuthenticatedMobileActor actor,
            UUID currentSessionId,
            PhoneVerificationConfirmRequest request) {
        requireActor(actor);
        PhoneOtpChallenge challenge = repository.findByIdForUpdate(request.challengeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_OTP_INVALID));
        OffsetDateTime now = now();
        if (challenge.isConsumed()) {
            throw new BusinessException(ErrorCode.PHONE_OTP_INVALID);
        }
        if (challenge.isExpired(now)) {
            throw new BusinessException(ErrorCode.PHONE_OTP_EXPIRED);
        }
        if (!challenge.hasAttemptsRemaining()) {
            throw new BusinessException(ErrorCode.PHONE_OTP_ATTEMPTS_EXCEEDED);
        }
        if (!tokenHashService.hash(request.code()).equals(challenge.getCodeHash())) {
            challenge.failAttempt();
            throw new BusinessException(ErrorCode.PHONE_OTP_INVALID);
        }
        if (challenge.getActorType() != actor.actorType()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (challenge.getPurpose() != PhoneOtpPurpose.CHANGE_PHONE && challenge.getPurpose() != PhoneOtpPurpose.LINK_PHONE) {
            throw new BusinessException(ErrorCode.PHONE_OTP_INVALID);
        }

        String normalizedPhone = challenge.getPhone();

        if (actor.actorType() == ActorType.CUSTOMER) {
            customerRepository.findByPhoneForUpdate(normalizedPhone)
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(actor.actorId())) {
                            throw new BusinessException(ErrorCode.CUSTOMER_PHONE_ALREADY_EXISTS);
                        }
                    });
            identityRepository.findActiveByProviderForUpdate(ActorType.CUSTOMER, MobileAuthProvider.PHONE, normalizedPhone)
                    .ifPresent(existing -> {
                        if (!existing.getActorId().equals(actor.actorId())) {
                            throw new BusinessException(ErrorCode.CUSTOMER_PHONE_ALREADY_EXISTS);
                        }
                    });

            Customer customer = customerRepository.findByIdForUpdate(actor.actorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE));
            customer.setPhone(normalizedPhone, now, now);

            identityRepository.findActiveActorProviderForUpdate(ActorType.CUSTOMER, customer.getId(), MobileAuthProvider.PHONE)
                    .ifPresentOrElse(
                            existing -> existing.updatePhone(normalizedPhone, now),
                            () -> identityRepository.saveAndFlush(MobileAuthIdentity.forCustomer(
                                    customer,
                                    MobileAuthProvider.PHONE,
                                    normalizedPhone,
                                    null,
                                    normalizedPhone,
                                    now)));
        } else {
            technicianRepository.findByPhoneForUpdate(normalizedPhone)
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(actor.actorId())) {
                            throw new BusinessException(ErrorCode.TECHNICIAN_PHONE_ALREADY_EXISTS);
                        }
                    });
            identityRepository.findActiveByProviderForUpdate(ActorType.TECHNICIAN, MobileAuthProvider.PHONE, normalizedPhone)
                    .ifPresent(existing -> {
                        if (!existing.getActorId().equals(actor.actorId())) {
                            throw new BusinessException(ErrorCode.TECHNICIAN_PHONE_ALREADY_EXISTS);
                        }
                    });

            Technician technician = technicianRepository.findByIdForUpdate(actor.actorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE));
            technician.setPhone(normalizedPhone, now, now);

            identityRepository.findActiveActorProviderForUpdate(ActorType.TECHNICIAN, technician.getId(), MobileAuthProvider.PHONE)
                    .ifPresentOrElse(
                            existing -> existing.updatePhone(normalizedPhone, now),
                            () -> identityRepository.saveAndFlush(MobileAuthIdentity.forTechnician(
                                    technician,
                                    MobileAuthProvider.PHONE,
                                    normalizedPhone,
                                    null,
                                    normalizedPhone,
                                    now)));
        }

        challenge.consume(now);

        // Security Policy Option A: Revoke all other mobile sessions and refresh families, keep current session alive
        if (refreshSessionService != null) {
            refreshSessionService.revokeOtherFamiliesForActor(
                    currentSessionId,
                    actor.actorType(),
                    actor.actorId(),
                    MobileRefreshRevocationReason.CREDENTIAL_CHANGED);
        }
    }

    @Transactional
    public void removePhone(AuthenticatedMobileActor actor, UUID currentSessionId) {
        requireActor(actor);
        OffsetDateTime now = now();

        if (actor.actorType() == ActorType.TECHNICIAN) {
            throw new BusinessException(ErrorCode.LAST_AUTH_METHOD);
        }

        List<MobileAuthIdentity> activeIdentities = identityRepository.findActiveForActor(actor.actorType(), actor.actorId());
        if (activeIdentities.size() <= 1) {
            throw new BusinessException(ErrorCode.LAST_AUTH_METHOD);
        }

        Customer customer = customerRepository.findByIdForUpdate(actor.actorId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE));
        customer.removePhone(now);

        identityRepository.findActiveActorProviderForUpdate(ActorType.CUSTOMER, customer.getId(), MobileAuthProvider.PHONE)
                .ifPresent(identity -> identity.disable(now));

        if (refreshSessionService != null) {
            refreshSessionService.revokeOtherFamiliesForActor(
                    currentSessionId,
                    actor.actorType(),
                    actor.actorId(),
                    MobileRefreshRevocationReason.CREDENTIAL_CHANGED);
        }
    }

    private void requireActor(AuthenticatedMobileActor actor) {
        if (actor == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
