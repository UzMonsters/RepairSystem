package com.example.darks.repair_auto.identity.mobile.otp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.example.darks.repair_auto.identity.mobile.auth.dto.PhoneOtpRequest;
import com.example.darks.repair_auto.identity.mobile.auth.dto.PhoneOtpResponse;
import com.example.darks.repair_auto.identity.mobile.auth.dto.PhoneVerificationConfirmRequest;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.shared.phone.PhoneNumberNormalizer;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PhoneOtpServiceTest {

    private PhoneOtpChallengeRepository repository;
    private PhoneNumberNormalizer phoneNumberNormalizer;
    private TokenHashService tokenHashService;
    private PhoneOtpProperties properties;
    private MobileAuthenticationService authenticationService;
    private SmsSender smsSender;
    private CustomerRepository customerRepository;
    private TechnicianRepository technicianRepository;
    private MobileAuthIdentityRepository identityRepository;
    private MobileSessionService mobileSessionService;
    private MobileRefreshSessionService refreshSessionService;
    private Clock clock;
    private PhoneOtpService service;

    private final Instant nowInstant = Instant.parse("2026-08-24T10:00:00Z");
    private final OffsetDateTime nowUtc = OffsetDateTime.ofInstant(nowInstant, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        repository = mock(PhoneOtpChallengeRepository.class);
        phoneNumberNormalizer = new PhoneNumberNormalizer();
        tokenHashService = new TokenHashService();
        properties = new PhoneOtpProperties();
        properties.setSmsEnabled(true);
        authenticationService = mock(MobileAuthenticationService.class);
        smsSender = mock(SmsSender.class);
        when(smsSender.isEnabled()).thenReturn(true);
        customerRepository = mock(CustomerRepository.class);
        technicianRepository = mock(TechnicianRepository.class);
        identityRepository = mock(MobileAuthIdentityRepository.class);
        mobileSessionService = mock(MobileSessionService.class);
        refreshSessionService = mock(MobileRefreshSessionService.class);
        clock = Clock.fixed(nowInstant, ZoneOffset.UTC);

        service = new PhoneOtpService(
                repository,
                phoneNumberNormalizer,
                tokenHashService,
                properties,
                authenticationService,
                smsSender,
                customerRepository,
                technicianRepository,
                identityRepository,
                mobileSessionService,
                refreshSessionService,
                clock);
    }

    @Test
    void givenPhoneWhenRequestOtpThenSavesChallengeAndDispatchesSms() {
        when(repository.findTopByPhoneAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(repository.save(any(PhoneOtpChallenge.class))).thenAnswer(i -> i.getArgument(0));

        PhoneOtpResponse response = service.request(new PhoneOtpRequest(PushClientType.CUSTOMER_MOBILE, "+998901234567"), "127.0.0.1", "TestAgent");

        assertThat(response.deliveryStatus()).isEqualTo("SENT");
        verify(smsSender).sendOtp(eq("+998901234567"), any(), eq(LanguageCode.UZ));
    }

    @Test
    void givenValidCodeWhenVerifyAndChangePhoneThenUpdatesCustomerAndRevokesOtherSessions() {
        Customer customer = Customer.google("Customer", "test@example.com", nowUtc, LanguageCode.UZ, nowUtc);
        ReflectionTestUtils.setField(customer, "id", 100L);

        String rawCode = "123456";
        String codeHash = tokenHashService.hash(rawCode);
        PhoneOtpChallenge challenge = new PhoneOtpChallenge(
                "+998909998877",
                ActorType.CUSTOMER,
                PushClientType.CUSTOMER_MOBILE,
                PhoneOtpPurpose.CHANGE_PHONE,
                codeHash,
                5,
                nowUtc,
                nowUtc.plusMinutes(5),
                nowUtc.plusSeconds(60),
                "127.0.0.1",
                "TestAgent");

        UUID challengeId = challenge.getId();
        when(repository.findByIdForUpdate(challengeId)).thenReturn(Optional.of(challenge));
        when(customerRepository.findByPhoneForUpdate("+998909998877")).thenReturn(Optional.empty());
        when(identityRepository.findActiveByProviderForUpdate(ActorType.CUSTOMER, MobileAuthProvider.PHONE, "+998909998877")).thenReturn(Optional.empty());
        when(customerRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(customer));
        when(identityRepository.findActiveActorProviderForUpdate(ActorType.CUSTOMER, 100L, MobileAuthProvider.PHONE)).thenReturn(Optional.empty());

        UUID currentSessionId = UUID.randomUUID();
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 100L, "customer:100", true, currentSessionId, PushClientType.CUSTOMER_MOBILE);

        service.verifyAndChangePhone(actor, currentSessionId, new PhoneVerificationConfirmRequest(challengeId, rawCode));

        assertThat(customer.getPhone()).isEqualTo("+998909998877");
        assertThat(customer.getPhoneVerifiedAt()).isNotNull();
        assertThat(challenge.isConsumed()).isTrue();
        verify(identityRepository).saveAndFlush(any(MobileAuthIdentity.class));
        verify(refreshSessionService).revokeOtherFamiliesForActor(
                eq(currentSessionId),
                eq(ActorType.CUSTOMER),
                eq(100L),
                eq(MobileRefreshRevocationReason.CREDENTIAL_CHANGED));
    }

    @Test
    void givenCustomerWithMultipleAuthMethodsWhenRemovePhoneThenSucceeds() {
        Customer customer = Customer.google("Customer", "test@example.com", nowUtc, LanguageCode.UZ, nowUtc);
        ReflectionTestUtils.setField(customer, "id", 100L);
        customer.setPhone("+998901234567", nowUtc, nowUtc);

        MobileAuthIdentity phoneIdentity = MobileAuthIdentity.forCustomer(customer, MobileAuthProvider.PHONE, "+998901234567", null, "+998901234567", nowUtc);
        MobileAuthIdentity googleIdentity = MobileAuthIdentity.forCustomer(customer, MobileAuthProvider.GOOGLE, "sub123", "test@example.com", null, nowUtc);

        when(identityRepository.findActiveForActor(ActorType.CUSTOMER, 100L)).thenReturn(List.of(phoneIdentity, googleIdentity));
        when(customerRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(customer));
        when(identityRepository.findActiveActorProviderForUpdate(ActorType.CUSTOMER, 100L, MobileAuthProvider.PHONE)).thenReturn(Optional.of(phoneIdentity));

        UUID currentSessionId = UUID.randomUUID();
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 100L, "customer:100", true, currentSessionId, PushClientType.CUSTOMER_MOBILE);

        service.removePhone(actor, currentSessionId);

        assertThat(customer.getPhone()).isNull();
        assertThat(customer.getPhoneVerifiedAt()).isNull();
        assertThat(phoneIdentity.isActive()).isFalse();
        verify(refreshSessionService).revokeOtherFamiliesForActor(
                eq(currentSessionId),
                eq(ActorType.CUSTOMER),
                eq(100L),
                eq(MobileRefreshRevocationReason.CREDENTIAL_CHANGED));
    }

    @Test
    void givenCustomerWithOnlyPhoneAuthWhenRemovePhoneThenThrowsLastAuthMethodException() {
        Customer customer = new Customer("Customer", "+998901234567", LanguageCode.UZ, nowUtc);
        ReflectionTestUtils.setField(customer, "id", 100L);

        MobileAuthIdentity phoneIdentity = MobileAuthIdentity.forCustomer(customer, MobileAuthProvider.PHONE, "+998901234567", null, "+998901234567", nowUtc);
        when(identityRepository.findActiveForActor(ActorType.CUSTOMER, 100L)).thenReturn(List.of(phoneIdentity));

        UUID currentSessionId = UUID.randomUUID();
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 100L, "customer:100", true, currentSessionId, PushClientType.CUSTOMER_MOBILE);

        assertThatThrownBy(() -> service.removePhone(actor, currentSessionId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.LAST_AUTH_METHOD);
    }
}
