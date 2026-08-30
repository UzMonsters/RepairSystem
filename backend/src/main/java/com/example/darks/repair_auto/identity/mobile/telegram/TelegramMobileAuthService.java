package com.example.darks.repair_auto.identity.mobile.telegram;

import static com.example.darks.repair_auto.identity.mobile.auth.MobileAuthLogSupport.deviceSummary;
import static com.example.darks.repair_auto.identity.mobile.auth.MobileAuthLogSupport.present;
import static com.example.darks.repair_auto.identity.mobile.auth.MobileAuthLogSupport.safeLength;
import static com.example.darks.repair_auto.identity.mobile.auth.MobileAuthLogSupport.safeUserAgent;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.application.ActorAccessLifecycleService;
import com.example.darks.repair_auto.identity.application.MobileRefreshSessionService;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.MobileAuthProvider;
import com.example.darks.repair_auto.identity.domain.MobileSession;
import com.example.darks.repair_auto.identity.domain.MobileRefreshRevocationReason;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.infrastructure.security.JwtTokenService;
import com.example.darks.repair_auto.identity.mobile.auth.MobileAuthenticationService;
import com.example.darks.repair_auto.identity.mobile.auth.VerifiedMobileIdentity;
import com.example.darks.repair_auto.identity.mobile.auth.dto.MobileDeviceContextRequest;
import com.example.darks.repair_auto.identity.mobile.telegram.dto.MobileActorSummary;
import com.example.darks.repair_auto.identity.mobile.telegram.dto.MobileAuthResponse;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.notification.push.domain.PushPlatform;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelegramMobileAuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramMobileAuthService.class);

    private final TelegramIdTokenVerifier idTokenVerifier;
    private final CustomerRepository customerRepository;
    private final TechnicianRepository technicianRepository;
    private final JwtTokenService jwtTokenService;
    private final MobileRefreshSessionService mobileRefreshSessionService;
    private final ActorAccessLifecycleService actorAccessLifecycleService;
    private final MobileAuthenticationService mobileAuthenticationService;

    @Autowired
    public TelegramMobileAuthService(
            TelegramIdTokenVerifier idTokenVerifier,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            JwtTokenService jwtTokenService,
            MobileRefreshSessionService mobileRefreshSessionService,
            ActorAccessLifecycleService actorAccessLifecycleService,
            MobileAuthenticationService mobileAuthenticationService) {
        this.idTokenVerifier = idTokenVerifier;
        this.customerRepository = customerRepository;
        this.technicianRepository = technicianRepository;
        this.jwtTokenService = jwtTokenService;
        this.mobileRefreshSessionService = mobileRefreshSessionService;
        this.actorAccessLifecycleService = actorAccessLifecycleService;
        this.mobileAuthenticationService = mobileAuthenticationService;
    }

    public TelegramMobileAuthService(
            TelegramIdTokenVerifier idTokenVerifier,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            JwtTokenService jwtTokenService,
            MobileRefreshSessionService mobileRefreshSessionService) {
        this(idTokenVerifier, customerRepository, technicianRepository, jwtTokenService, mobileRefreshSessionService, null, null);
    }

    @Transactional
    public MobileAuthResponse loginCustomer(String idToken) {
        return loginCustomer(idToken, null, null, null);
    }

    @Transactional
    public MobileAuthResponse loginCustomer(String idToken, MobileDeviceContextRequest device, String ip, String userAgent) {
        LOGGER.info(
                "Mobile Telegram customer login verification started tokenPresent={} tokenLength={} "
                        + "device=[{}] ip={} userAgent={}",
                present(idToken),
                safeLength(idToken),
                deviceSummary(device),
                ip,
                safeUserAgent(userAgent));
        TelegramIdentity identity = idTokenVerifier.verifyCustomerToken(idToken);
        LOGGER.info(
                "Mobile Telegram customer token verified telegramUserId={} subjectPresent={} usernamePresent={}",
                identity.telegramUserId(),
                present(identity.subject()),
                present(identity.username()));
        if (mobileAuthenticationService != null) {
            return mobileAuthenticationService.authenticate(
                    PushClientType.CUSTOMER_MOBILE,
                    new VerifiedMobileIdentity(
                            MobileAuthProvider.TELEGRAM,
                            String.valueOf(identity.telegramUserId()),
                            null,
                            false,
                            identity.phoneNumber(),
                            identity.phoneNumberVerified(),
                            identity.name()),
                    device,
                    ip,
                    userAgent);
        }
        Long telegramUserId = identity.telegramUserId();

        Customer customer = customerRepository.findByTelegramUserId(telegramUserId)
                .orElseThrow(() -> {
                    LOGGER.info("Mobile customer login rejected: unlinked telegramUserId={}", telegramUserId);
                    return new BusinessException(ErrorCode.TELEGRAM_ACCOUNT_NOT_LINKED);
                });

        if (!customer.isActive()) {
            LOGGER.info("Mobile customer login rejected: customerId={} is inactive", customer.getId());
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }

        String subject = (customer.getPhone() != null && !customer.getPhone().isBlank())
                ? customer.getPhone()
                : "customer:" + customer.getId();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        MobileSession session = MobileSession.forCustomer(
                customer,
                MobileAuthProvider.TELEGRAM,
                PushPlatform.ANDROID,
                null,
                null,
                null,
                "127.0.0.1",
                "Telegram-Mobile",
                now,
                now.plusDays(30));

        String accessToken = jwtTokenService.issueMobile(
                ActorType.CUSTOMER,
                customer.getId(),
                customer.getAuthVersion(),
                session.getId(),
                PushClientType.CUSTOMER_MOBILE,
                subject);
        MobileRefreshSessionService.IssuedMobileRefreshToken refresh = mobileRefreshSessionService.createForCustomer(customer, session);

        LOGGER.info("Mobile customer login successful: customerId={}", customer.getId());

        String lang = customer.getPreferredLanguage() != null
                ? customer.getPreferredLanguage().name().toLowerCase(Locale.ROOT)
                : "uz";

        return new MobileAuthResponse(
                "Bearer",
                accessToken,
                refresh.rawToken(),
                jwtTokenService.accessTokenTtlSeconds(),
                mobileRefreshSessionService.remainingTtlSeconds(refresh.session()),
                new MobileActorSummary(
                        ActorType.CUSTOMER,
                        customer.getId(),
                        customer.getFullName(),
                        customer.getPhone(),
                        lang),
                new MobileAuthResponse.MobileSessionSummary(session.getId()));
    }

    @Transactional
    public MobileAuthResponse loginTechnician(String idToken) {
        return loginTechnician(idToken, null, null, null);
    }

    @Transactional
    public MobileAuthResponse loginTechnician(String idToken, MobileDeviceContextRequest device, String ip, String userAgent) {
        LOGGER.info(
                "Mobile Telegram technician login verification started tokenPresent={} tokenLength={} "
                        + "device=[{}] ip={} userAgent={}",
                present(idToken),
                safeLength(idToken),
                deviceSummary(device),
                ip,
                safeUserAgent(userAgent));
        TelegramIdentity identity = idTokenVerifier.verifyTechnicianToken(idToken);
        LOGGER.info(
                "Mobile Telegram technician token verified telegramUserId={} subjectPresent={} usernamePresent={}",
                identity.telegramUserId(),
                present(identity.subject()),
                present(identity.username()));
        if (mobileAuthenticationService != null) {
            return mobileAuthenticationService.authenticate(
                    PushClientType.TECHNICIAN_MOBILE,
                    new VerifiedMobileIdentity(
                            MobileAuthProvider.TELEGRAM,
                            String.valueOf(identity.telegramUserId()),
                            null,
                            false,
                            identity.phoneNumber(),
                            identity.phoneNumberVerified(),
                            identity.name()),
                    device,
                    ip,
                    userAgent);
        }
        Long telegramUserId = identity.telegramUserId();

        Technician technician = technicianRepository.findByTelegramUserId(telegramUserId)
                .orElseThrow(() -> {
                    LOGGER.info("Mobile technician login rejected: unlinked telegramUserId={}", telegramUserId);
                    return new BusinessException(ErrorCode.TELEGRAM_ACCOUNT_NOT_LINKED);
                });

        if (!technician.isActive()) {
            LOGGER.info("Mobile technician login rejected: technicianId={} is inactive", technician.getId());
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }

        String subject = (technician.getPhone() != null && !technician.getPhone().isBlank())
                ? technician.getPhone()
                : "technician:" + technician.getId();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        MobileSession session = MobileSession.forTechnician(
                technician,
                MobileAuthProvider.TELEGRAM,
                PushPlatform.ANDROID,
                null,
                null,
                null,
                "127.0.0.1",
                "Telegram-Mobile",
                now,
                now.plusDays(30));

        String accessToken = jwtTokenService.issueMobile(
                ActorType.TECHNICIAN,
                technician.getId(),
                technician.getAuthVersion(),
                session.getId(),
                PushClientType.TECHNICIAN_MOBILE,
                subject);
        MobileRefreshSessionService.IssuedMobileRefreshToken refresh = mobileRefreshSessionService.createForTechnician(technician, session);

        LOGGER.info("Mobile technician login successful: technicianId={}", technician.getId());

        String lang = technician.getPreferredLanguage() != null
                ? technician.getPreferredLanguage().name().toLowerCase(Locale.ROOT)
                : "uz";

        return new MobileAuthResponse(
                "Bearer",
                accessToken,
                refresh.rawToken(),
                jwtTokenService.accessTokenTtlSeconds(),
                mobileRefreshSessionService.remainingTtlSeconds(refresh.session()),
                new MobileActorSummary(
                        ActorType.TECHNICIAN,
                        technician.getId(),
                        technician.getFullName(),
                        technician.getPhone(),
                        lang),
                new MobileAuthResponse.MobileSessionSummary(session.getId()));
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public MobileAuthResponse refresh(String rawRefreshToken) {
        MobileRefreshSessionService.MobileRotationResult rotation = mobileRefreshSessionService.rotate(rawRefreshToken);

        String subject = (rotation.phone() != null && !rotation.phone().isBlank())
                ? rotation.phone()
                : (rotation.actorType() == ActorType.CUSTOMER ? "customer:" : "technician:") + rotation.actorId();

        MobileSession mobileSession = rotation.session().getMobileSession();
        if (mobileSession == null) {
            throw new BusinessException(ErrorCode.MOBILE_REFRESH_TOKEN_EXPIRED);
        }
        Long authVersion = 0L;
        if (rotation.actorType() == ActorType.CUSTOMER && rotation.customer() != null) {
            authVersion = rotation.customer().getAuthVersion();
        } else if (rotation.actorType() == ActorType.TECHNICIAN && rotation.technician() != null) {
            authVersion = rotation.technician().getAuthVersion();
        }
        String newAccessToken = jwtTokenService.issueMobile(
                rotation.actorType(),
                rotation.actorId(),
                authVersion,
                mobileSession.getId(),
                mobileSession.getClientType(),
                subject);

        String lang;
        if (rotation.actorType() == ActorType.CUSTOMER && rotation.customer() != null && rotation.customer().getPreferredLanguage() != null) {
            lang = rotation.customer().getPreferredLanguage().name().toLowerCase(Locale.ROOT);
        } else if (rotation.actorType() == ActorType.TECHNICIAN && rotation.technician() != null && rotation.technician().getPreferredLanguage() != null) {
            lang = rotation.technician().getPreferredLanguage().name().toLowerCase(Locale.ROOT);
        } else {
            lang = "uz";
        }

        return new MobileAuthResponse(
                "Bearer",
                newAccessToken,
                rotation.rawRefreshToken(),
                jwtTokenService.accessTokenTtlSeconds(),
                mobileRefreshSessionService.remainingTtlSeconds(rotation.session()),
                new MobileActorSummary(
                        rotation.actorType(),
                        rotation.actorId(),
                        rotation.fullName(),
                        rotation.phone(),
                        lang),
                new MobileAuthResponse.MobileSessionSummary(mobileSession.getId()));
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        mobileRefreshSessionService.revokeByRawToken(rawRefreshToken);
    }

    @Transactional
    public void logoutAll(AuthenticatedMobileActor actor) {
        if (actor == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        if (actor.isCustomer()) {
            if (actorAccessLifecycleService != null) {
                actorAccessLifecycleService.onCustomerLogoutAll(actor.actorId());
            } else {
                mobileRefreshSessionService.revokeAllForCustomer(actor.actorId(), MobileRefreshRevocationReason.LOGOUT_ALL);
            }
        } else if (actor.isTechnician()) {
            if (actorAccessLifecycleService != null) {
                actorAccessLifecycleService.onTechnicianLogoutAll(actor.actorId());
            } else {
                mobileRefreshSessionService.revokeAllForTechnician(actor.actorId(), MobileRefreshRevocationReason.LOGOUT_ALL);
            }
        }
    }
}
