package com.example.darks.repair_auto.identity.mobile.email;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.application.EmailNormalizer;
import com.example.darks.repair_auto.identity.application.TokenHashService;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.EmailVerificationChallenge;
import com.example.darks.repair_auto.identity.infrastructure.persistence.EmailVerificationChallengeRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.mobile.email.dto.EmailVerificationConfirmRequest;
import com.example.darks.repair_auto.identity.mobile.email.dto.EmailVerificationRequest;
import com.example.darks.repair_auto.identity.mobile.email.dto.EmailVerificationResponse;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {

    private final EmailVerificationChallengeRepository challengeRepository;
    private final CustomerRepository customerRepository;
    private final TechnicianRepository technicianRepository;
    private final EmailNormalizer emailNormalizer;
    private final TokenHashService tokenHashService;
    private final EmailVerificationProperties properties;
    private final EmailSender emailSender;
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    @Autowired
    public EmailVerificationService(
            EmailVerificationChallengeRepository challengeRepository,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            EmailNormalizer emailNormalizer,
            TokenHashService tokenHashService,
            EmailVerificationProperties properties,
            EmailSender emailSender) {
        this(challengeRepository, customerRepository, technicianRepository, emailNormalizer, tokenHashService, properties, emailSender, Clock.systemUTC());
    }

    EmailVerificationService(
            EmailVerificationChallengeRepository challengeRepository,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            EmailNormalizer emailNormalizer,
            TokenHashService tokenHashService,
            EmailVerificationProperties properties,
            EmailSender emailSender,
            Clock clock) {
        this.challengeRepository = challengeRepository;
        this.customerRepository = customerRepository;
        this.technicianRepository = technicianRepository;
        this.emailNormalizer = emailNormalizer;
        this.tokenHashService = tokenHashService;
        this.properties = properties;
        this.emailSender = emailSender;
        this.clock = clock;
    }

    @Transactional
    public EmailVerificationResponse request(AuthenticatedMobileActor actor, EmailVerificationRequest request) {
        requireActor(actor);
        OffsetDateTime now = now();
        String normalizedEmail = emailNormalizer.normalize(request.email());

        if (actor.isCustomer()) {
            challengeRepository.findTopActiveByCustomer(actor.actorId(), now)
                    .ifPresent(existing -> {
                        if (existing.getResendAvailableAt().isAfter(now)) {
                            throw new BusinessException(ErrorCode.PHONE_OTP_RESEND_COOLDOWN);
                        }
                    });
        } else {
            challengeRepository.findTopActiveByTechnician(actor.actorId(), now)
                    .ifPresent(existing -> {
                        if (existing.getResendAvailableAt().isAfter(now)) {
                            throw new BusinessException(ErrorCode.PHONE_OTP_RESEND_COOLDOWN);
                        }
                    });
        }

        String code = "%06d".formatted(random.nextInt(1_000_000));
        EmailVerificationChallenge challenge;
        LanguageCode language;

        if (actor.isCustomer()) {
            Customer customer = customerRepository.findByIdForUpdate(actor.actorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE));
            if (!customer.isActive()) {
                throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
            }
            language = customer.getPreferredLanguage();
            challenge = EmailVerificationChallenge.forCustomer(
                    customer,
                    normalizedEmail,
                    tokenHashService.hash(code),
                    properties.getMaxAttempts(),
                    now,
                    now.plus(properties.getTtl()),
                    now.plus(properties.getResendCooldown()));
        } else {
            Technician technician = technicianRepository.findByIdForUpdate(actor.actorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE));
            if (!technician.isActive()) {
                throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
            }
            language = technician.getPreferredLanguage();
            challenge = EmailVerificationChallenge.forTechnician(
                    technician,
                    normalizedEmail,
                    tokenHashService.hash(code),
                    properties.getMaxAttempts(),
                    now,
                    now.plus(properties.getTtl()),
                    now.plus(properties.getResendCooldown()));
        }
        EmailVerificationChallenge saved = challengeRepository.save(challenge);

        String deliveryStatus = "DELIVERY_DISABLED";
        if (emailSender != null && emailSender.isEnabled()) {
            emailSender.sendVerificationCode(normalizedEmail, code, language);
            deliveryStatus = "SENT";
        }

        return new EmailVerificationResponse(
                saved.getId(),
                properties.getTtl().toSeconds(),
                properties.getResendCooldown().toSeconds(),
                deliveryStatus);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public void verify(AuthenticatedMobileActor actor, EmailVerificationConfirmRequest request) {
        requireActor(actor);
        EmailVerificationChallenge challenge = challengeRepository.findByIdForUpdate(request.challengeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_VERIFICATION_INVALID));
        OffsetDateTime now = now();
        if (challenge.isConsumed()) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_INVALID);
        }
        if (challenge.isExpired(now)) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_EXPIRED);
        }
        if (!challenge.hasAttemptsRemaining()) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_INVALID);
        }
        if (!tokenHashService.hash(request.code()).equals(challenge.getCodeHash())) {
            challenge.failAttempt();
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_INVALID);
        }
        if (actor.actorType() != challenge.getActorType()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (actor.actorType() == ActorType.CUSTOMER) {
            Customer customer = challenge.getCustomer();
            if (!customer.getId().equals(actor.actorId())) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
            }
            customer.setEmail(challenge.getPendingEmail(), now, now);
        } else {
            Technician technician = challenge.getTechnician();
            if (!technician.getId().equals(actor.actorId())) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
            }
            technician.setEmail(challenge.getPendingEmail(), now, now);
        }
        challenge.consume(now);
    }

    @Transactional
    public void remove(AuthenticatedMobileActor actor) {
        requireActor(actor);
        if (actor.isCustomer()) {
            customerRepository.findByIdForUpdate(actor.actorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE))
                    .removeEmail(now());
        } else {
            technicianRepository.findByIdForUpdate(actor.actorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE))
                    .removeEmail(now());
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
