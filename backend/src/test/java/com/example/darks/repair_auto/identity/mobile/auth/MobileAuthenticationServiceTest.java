package com.example.darks.repair_auto.identity.mobile.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.phone.PhoneNumberNormalizer;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MobileAuthenticationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-28T10:00:00Z");

    private MobileAuthIdentityRepository identityRepository;
    private CustomerRepository customerRepository;
    private MobileSessionService mobileSessionService;
    private MobileRefreshSessionService refreshSessionService;
    private JwtTokenService jwtTokenService;
    private MobileAuthenticationService service;

    @BeforeEach
    void setUp() {
        identityRepository = mock(MobileAuthIdentityRepository.class);
        customerRepository = mock(CustomerRepository.class);
        TechnicianRepository technicianRepository = mock(TechnicianRepository.class);
        mobileSessionService = mock(MobileSessionService.class);
        refreshSessionService = mock(MobileRefreshSessionService.class);
        jwtTokenService = mock(JwtTokenService.class);

        service = new MobileAuthenticationService(
                identityRepository,
                customerRepository,
                technicianRepository,
                mobileSessionService,
                refreshSessionService,
                jwtTokenService,
                new EmailNormalizer(),
                new PhoneNumberNormalizer(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void givenTelegramCustomerWithVerifiedUzbekPhoneWhenAuthenticateThenRegistersWithVerifiedPhone() {
        VerifiedMobileIdentity telegram = new VerifiedMobileIdentity(
                MobileAuthProvider.TELEGRAM,
                "1122334455",
                null,
                false,
                "998901234567",
                true,
                "Telegram Customer");

        Customer savedCustomer = Customer.telegram(
                "Telegram Customer",
                "+998901234567",
                1122334455L,
                null,
                com.example.darks.repair_auto.shared.i18n.LanguageCode.UZ,
                now());
        ReflectionTestUtils.setField(savedCustomer, "id", 42L);

        MobileSession session = mock(MobileSession.class);
        UUID sessionId = UUID.randomUUID();
        MobileRefreshSession refreshSession = mock(MobileRefreshSession.class);

        when(identityRepository.findActiveByProviderForUpdate(ActorType.CUSTOMER, MobileAuthProvider.TELEGRAM, "1122334455"))
                .thenReturn(Optional.empty());
        when(customerRepository.findByTelegramUserIdForUpdate(1122334455L)).thenReturn(Optional.empty());
        when(customerRepository.saveAndFlush(any(Customer.class))).thenReturn(savedCustomer);
        when(identityRepository.findActiveActorProviderForUpdate(ActorType.CUSTOMER, 42L, MobileAuthProvider.TELEGRAM))
                .thenReturn(Optional.empty());
        when(mobileSessionService.createForCustomer(eq(savedCustomer), eq(MobileAuthProvider.TELEGRAM), any(), any(), any()))
                .thenReturn(session);
        when(session.getId()).thenReturn(sessionId);
        when(session.getClientType()).thenReturn(PushClientType.CUSTOMER_MOBILE);
        when(refreshSessionService.createForCustomer(savedCustomer, session))
                .thenReturn(new MobileRefreshSessionService.IssuedMobileRefreshToken("refresh", refreshSession));
        when(refreshSessionService.remainingTtlSeconds(refreshSession)).thenReturn(3600L);
        when(jwtTokenService.issueMobile(
                ActorType.CUSTOMER,
                42L,
                0L,
                sessionId,
                PushClientType.CUSTOMER_MOBILE,
                "+998901234567"))
                .thenReturn("access");
        when(jwtTokenService.accessTokenTtlSeconds()).thenReturn(900L);

        var response = service.authenticate(PushClientType.CUSTOMER_MOBILE, telegram, null, "127.0.0.1", "test");

        assertThat(response.actor().phone()).isEqualTo("+998901234567");
        assertThat(savedCustomer.getPhoneVerifiedAt()).isNotNull();
        verify(identityRepository).saveAndFlush(any(MobileAuthIdentity.class));
    }

    @Test
    void givenTelegramCustomerWithoutPhoneWhenAuthenticateThenFails() {
        VerifiedMobileIdentity telegram = telegramIdentity(null, false);

        BusinessException exception = catchThrowableOfType(
                () -> service.authenticate(PushClientType.CUSTOMER_MOBILE, telegram, null, null, null),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PHONE_REQUIRED);
        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void givenTelegramCustomerWithUnverifiedPhoneWhenAuthenticateThenFails() {
        VerifiedMobileIdentity telegram = telegramIdentity("+998901234567", false);

        BusinessException exception = catchThrowableOfType(
                () -> service.authenticate(PushClientType.CUSTOMER_MOBILE, telegram, null, null, null),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TELEGRAM_AUTH_INVALID);
        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void givenTelegramCustomerWithNonUzbekPhoneWhenAuthenticateThenFails() {
        VerifiedMobileIdentity telegram = telegramIdentity("+971577777777", true);

        BusinessRuleException exception = catchThrowableOfType(
                () -> service.authenticate(PushClientType.CUSTOMER_MOBILE, telegram, null, null, null),
                BusinessRuleException.class);

        assertThat(exception.code()).isEqualTo("INVALID_PHONE_NUMBER");
        verify(customerRepository, never()).saveAndFlush(any());
    }

    private VerifiedMobileIdentity telegramIdentity(String phone, boolean phoneVerified) {
        return new VerifiedMobileIdentity(
                MobileAuthProvider.TELEGRAM,
                "1122334455",
                null,
                false,
                phone,
                phoneVerified,
                "Telegram Customer");
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);
    }
}
