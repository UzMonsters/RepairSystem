package com.example.darks.repair_auto.identity.mobile.auth;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.application.EmailNormalizer;
import com.example.darks.repair_auto.identity.application.MobileRefreshSessionService;
import com.example.darks.repair_auto.identity.application.MobileSessionService;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.MobileAuthIdentity;
import com.example.darks.repair_auto.identity.domain.MobileAuthProvider;
import com.example.darks.repair_auto.identity.domain.MobileRefreshSession;
import com.example.darks.repair_auto.identity.domain.MobileSession;
import com.example.darks.repair_auto.identity.infrastructure.persistence.MobileAuthIdentityRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.JwtTokenService;
import com.example.darks.repair_auto.identity.mobile.auth.dto.MobileDeviceContextRequest;
import com.example.darks.repair_auto.identity.mobile.telegram.dto.MobileActorSummary;
import com.example.darks.repair_auto.identity.mobile.telegram.dto.MobileAuthResponse;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.shared.phone.PhoneNumberNormalizer;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MobileAuthenticationService {

    private final MobileAuthIdentityRepository identityRepository;
    private final CustomerRepository customerRepository;
    private final TechnicianRepository technicianRepository;
    private final MobileSessionService mobileSessionService;
    private final MobileRefreshSessionService refreshSessionService;
    private final JwtTokenService jwtTokenService;
    private final EmailNormalizer emailNormalizer;
    private final PhoneNumberNormalizer phoneNumberNormalizer;
    private final Clock clock;

    @Autowired
    public MobileAuthenticationService(
            MobileAuthIdentityRepository identityRepository,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            MobileSessionService mobileSessionService,
            MobileRefreshSessionService refreshSessionService,
            JwtTokenService jwtTokenService,
            EmailNormalizer emailNormalizer,
            PhoneNumberNormalizer phoneNumberNormalizer) {
        this(identityRepository, customerRepository, technicianRepository, mobileSessionService, refreshSessionService,
                jwtTokenService, emailNormalizer, phoneNumberNormalizer, Clock.systemUTC());
    }

    MobileAuthenticationService(
            MobileAuthIdentityRepository identityRepository,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            MobileSessionService mobileSessionService,
            MobileRefreshSessionService refreshSessionService,
            JwtTokenService jwtTokenService,
            EmailNormalizer emailNormalizer,
            PhoneNumberNormalizer phoneNumberNormalizer,
            Clock clock) {
        this.identityRepository = identityRepository;
        this.customerRepository = customerRepository;
        this.technicianRepository = technicianRepository;
        this.mobileSessionService = mobileSessionService;
        this.refreshSessionService = refreshSessionService;
        this.jwtTokenService = jwtTokenService;
        this.emailNormalizer = emailNormalizer;
        this.phoneNumberNormalizer = phoneNumberNormalizer;
        this.clock = clock;
    }

    @Transactional
    public MobileAuthResponse authenticate(
            PushClientType clientType,
            VerifiedMobileIdentity verifiedIdentity,
            MobileDeviceContextRequest device,
            String ip,
            String userAgent) {
        ActorType actorType = MobileClientTypeResolver.actorType(clientType);
        MobileAuthIdentity identity = identityRepository.findActiveByProviderForUpdate(
                        actorType,
                        verifiedIdentity.provider(),
                        verifiedIdentity.providerSubject())
                .orElse(null);
        if (identity != null) {
            identity.markUsed(now());
            return identity.getActorType() == ActorType.CUSTOMER
                    ? loginCustomer(identity.getCustomer(), verifiedIdentity.provider(), device, ip, userAgent)
                    : loginTechnician(identity.getTechnician(), verifiedIdentity.provider(), device, ip, userAgent);
        }
        return actorType == ActorType.CUSTOMER
                ? registerOrClaimCustomer(verifiedIdentity, device, ip, userAgent)
                : claimTechnician(verifiedIdentity, device, ip, userAgent);
    }

    private MobileAuthResponse registerOrClaimCustomer(
            VerifiedMobileIdentity identity,
            MobileDeviceContextRequest device,
            String ip,
            String userAgent) {
        OffsetDateTime now = now();
        Customer customer;
        String normalizedEmail = normalizeEmail(identity.email());
        String normalizedPhone = normalizePhoneOrNull(identity.phone());
        if (identity.provider() == MobileAuthProvider.PHONE) {
            customer = customerRepository.findByPhoneForUpdate(normalizedPhone)
                    .orElseGet(() -> customerRepository.saveAndFlush(Customer.phone(
                            fallbackName(identity, normalizedPhone),
                            normalizedPhone,
                            now,
                            LanguageCode.UZ,
                            now)));
            customer.markPhoneVerified(now);
        } else if (identity.provider() == MobileAuthProvider.TELEGRAM) {
            Long telegramUserId = parseLongSubject(identity.providerSubject());
            customer = customerRepository.findByTelegramUserIdForUpdate(telegramUserId)
                    .orElseGet(() -> customerRepository.saveAndFlush(Customer.telegram(
                            fallbackName(identity, "Telegram Customer"),
                            normalizedPhone,
                            telegramUserId,
                            null,
                            LanguageCode.UZ,
                            now)));
        } else {
            if (!identity.emailVerified() || normalizedEmail == null) {
                throw new BusinessException(ErrorCode.GOOGLE_EMAIL_NOT_VERIFIED);
            }
            Customer existingVerifiedEmail = customerRepository.findByEmailIgnoreCase(normalizedEmail)
                    .filter(existing -> existing.getEmailVerifiedAt() != null)
                    .orElse(null);
            if (existingVerifiedEmail != null) {
                throw new BusinessException(ErrorCode.ACCOUNT_LINK_REQUIRED);
            }
            customer = customerRepository.saveAndFlush(Customer.google(
                    fallbackName(identity, normalizedEmail),
                    normalizedEmail,
                    now,
                    LanguageCode.UZ,
                    now));
        }
        linkIdentity(customer, identity, normalizedEmail, normalizedPhone, now);
        return loginCustomer(customer, identity.provider(), device, ip, userAgent);
    }

    private MobileAuthResponse claimTechnician(
            VerifiedMobileIdentity identity,
            MobileDeviceContextRequest device,
            String ip,
            String userAgent) {
        OffsetDateTime now = now();
        Technician technician;
        String normalizedEmail = normalizeEmail(identity.email());
        String normalizedPhone = normalizePhoneOrNull(identity.phone());
        if (identity.provider() == MobileAuthProvider.PHONE) {
            technician = technicianRepository.findByPhoneForUpdate(normalizedPhone)
                    .filter(Technician::isActive)
                    .orElseThrow(() -> new BusinessException(ErrorCode.TECHNICIAN_ACCOUNT_NOT_PROVISIONED));
            technician.markPhoneVerified(now);
        } else if (identity.provider() == MobileAuthProvider.TELEGRAM) {
            technician = technicianRepository.findByTelegramUserIdForUpdate(parseLongSubject(identity.providerSubject()))
                    .filter(Technician::isActive)
                    .orElseThrow(() -> new BusinessException(ErrorCode.TELEGRAM_ACCOUNT_NOT_LINKED));
        } else {
            if (!identity.emailVerified() || normalizedEmail == null) {
                throw new BusinessException(ErrorCode.GOOGLE_EMAIL_NOT_VERIFIED);
            }
            List<Technician> matches = technicianRepository.findActiveByEmailForUpdate(normalizedEmail);
            if (matches.size() != 1) {
                throw new BusinessException(ErrorCode.TECHNICIAN_ACCOUNT_NOT_PROVISIONED);
            }
            technician = matches.get(0);
            technician.setEmail(normalizedEmail, now, now);
        }
        linkIdentity(technician, identity, normalizedEmail, normalizedPhone, now);
        return loginTechnician(technician, identity.provider(), device, ip, userAgent);
    }

    private void linkIdentity(
            Customer customer,
            VerifiedMobileIdentity identity,
            String normalizedEmail,
            String normalizedPhone,
            OffsetDateTime now) {
        if (identityRepository.findActiveActorProviderForUpdate(
                ActorType.CUSTOMER, customer.getId(), identity.provider()).isPresent()) {
            throw new BusinessException(ErrorCode.MOBILE_PROVIDER_CONFLICT);
        }
        try {
            identityRepository.saveAndFlush(MobileAuthIdentity.forCustomer(
                    customer,
                    identity.provider(),
                    identity.providerSubject(),
                    normalizedEmail,
                    normalizedPhone,
                    now));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.MOBILE_PROVIDER_CONFLICT);
        }
    }

    private void linkIdentity(
            Technician technician,
            VerifiedMobileIdentity identity,
            String normalizedEmail,
            String normalizedPhone,
            OffsetDateTime now) {
        if (identityRepository.findActiveActorProviderForUpdate(
                ActorType.TECHNICIAN, technician.getId(), identity.provider()).isPresent()) {
            throw new BusinessException(ErrorCode.MOBILE_PROVIDER_CONFLICT);
        }
        try {
            identityRepository.saveAndFlush(MobileAuthIdentity.forTechnician(
                    technician,
                    identity.provider(),
                    identity.providerSubject(),
                    normalizedEmail,
                    normalizedPhone,
                    now));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.MOBILE_PROVIDER_CONFLICT);
        }
    }

    private MobileAuthResponse loginCustomer(
            Customer customer,
            MobileAuthProvider provider,
            MobileDeviceContextRequest device,
            String ip,
            String userAgent) {
        if (!customer.isActive()) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }
        MobileSession session = mobileSessionService.createForCustomer(customer, provider, device, ip, userAgent);
        MobileRefreshSessionService.IssuedMobileRefreshToken refresh =
                refreshSessionService.createForCustomer(customer, session);
        return response(ActorType.CUSTOMER, customer, null, session, refresh);
    }

    private MobileAuthResponse loginTechnician(
            Technician technician,
            MobileAuthProvider provider,
            MobileDeviceContextRequest device,
            String ip,
            String userAgent) {
        if (!technician.isActive()) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }
        MobileSession session = mobileSessionService.createForTechnician(technician, provider, device, ip, userAgent);
        MobileRefreshSessionService.IssuedMobileRefreshToken refresh =
                refreshSessionService.createForTechnician(technician, session);
        return response(ActorType.TECHNICIAN, null, technician, session, refresh);
    }

    private MobileAuthResponse response(
            ActorType actorType,
            Customer customer,
            Technician technician,
            MobileSession session,
            MobileRefreshSessionService.IssuedMobileRefreshToken refresh) {
        Long actorId = actorType == ActorType.CUSTOMER ? customer.getId() : technician.getId();
        String subject = subject(actorType, customer, technician);
        long authVersion = actorType == ActorType.CUSTOMER ? customer.getAuthVersion() : technician.getAuthVersion();
        String accessToken = jwtTokenService.issueMobile(
                actorType,
                actorId,
                authVersion,
                session.getId(),
                session.getClientType(),
                subject);
        MobileRefreshSession refreshSession = refresh.session();
        return new MobileAuthResponse(
                "Bearer",
                accessToken,
                refresh.rawToken(),
                jwtTokenService.accessTokenTtlSeconds(),
                refreshSessionService.remainingTtlSeconds(refreshSession),
                new MobileActorSummary(
                        actorType,
                        actorId,
                        actorType == ActorType.CUSTOMER ? customer.getFullName() : technician.getFullName(),
                        actorType == ActorType.CUSTOMER ? customer.getPhone() : technician.getPhone(),
                        language(actorType == ActorType.CUSTOMER ? customer.getPreferredLanguage() : technician.getPreferredLanguage())),
                new MobileAuthResponse.MobileSessionSummary(session.getId()));
    }

    private String subject(ActorType actorType, Customer customer, Technician technician) {
        String phone = actorType == ActorType.CUSTOMER ? customer.getPhone() : technician.getPhone();
        if (phone != null && !phone.isBlank()) {
            return phone;
        }
        return actorType.name().toLowerCase(Locale.ROOT) + ":"
                + (actorType == ActorType.CUSTOMER ? customer.getId() : technician.getId());
    }

    private String fallbackName(VerifiedMobileIdentity identity, String fallback) {
        if (identity.displayName() != null && !identity.displayName().isBlank()) {
            return identity.displayName().trim();
        }
        return fallback == null || fallback.isBlank() ? "RepairAuto User" : fallback;
    }

    private String language(LanguageCode code) {
        return code == null ? "uz" : code.name().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return emailNormalizer.normalize(email);
    }

    private String normalizePhoneOrNull(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return phoneNumberNormalizer.normalize(phone);
    }

    private Long parseLongSubject(String subject) {
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
