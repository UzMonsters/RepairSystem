package com.example.darks.repair_auto.identity.mobile.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.application.EmailNormalizer;
import com.example.darks.repair_auto.identity.application.MobileRefreshSessionService;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.MobileAuthIdentity;
import com.example.darks.repair_auto.identity.domain.MobileAuthProvider;
import com.example.darks.repair_auto.identity.domain.MobileRefreshRevocationReason;
import com.example.darks.repair_auto.identity.infrastructure.persistence.MobileAuthIdentityRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.mobile.auth.dto.GoogleLinkRequest;
import com.example.darks.repair_auto.identity.mobile.auth.dto.MobileAuthMethodResponse;
import com.example.darks.repair_auto.identity.mobile.google.GoogleIdTokenVerifier;
import com.example.darks.repair_auto.identity.mobile.google.GoogleIdentity;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
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

class MobileAuthMethodServiceTest {

    private MobileAuthIdentityRepository identityRepository;
    private CustomerRepository customerRepository;
    private TechnicianRepository technicianRepository;
    private GoogleIdTokenVerifier googleIdTokenVerifier;
    private EmailNormalizer emailNormalizer;
    private MobileRefreshSessionService refreshSessionService;
    private Clock clock;
    private MobileAuthMethodService service;

    private final Instant nowInstant = Instant.parse("2026-08-24T10:00:00Z");
    private final OffsetDateTime nowUtc = OffsetDateTime.ofInstant(nowInstant, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        identityRepository = mock(MobileAuthIdentityRepository.class);
        customerRepository = mock(CustomerRepository.class);
        technicianRepository = mock(TechnicianRepository.class);
        googleIdTokenVerifier = mock(GoogleIdTokenVerifier.class);
        emailNormalizer = new EmailNormalizer();
        refreshSessionService = mock(MobileRefreshSessionService.class);
        clock = Clock.fixed(nowInstant, ZoneOffset.UTC);

        service = new MobileAuthMethodService(
                identityRepository,
                customerRepository,
                technicianRepository,
                googleIdTokenVerifier,
                emailNormalizer,
                refreshSessionService,
                clock);
    }

    @Test
    void givenActiveIdentitiesWhenListThenReturnsMaskedAuthMethods() {
        Customer customer = Customer.google("Customer", "john.doe@example.com", nowUtc, LanguageCode.UZ, nowUtc);
        ReflectionTestUtils.setField(customer, "id", 100L);
        customer.setPhone("+998901234567", nowUtc, nowUtc);

        MobileAuthIdentity phoneIdentity = MobileAuthIdentity.forCustomer(customer, MobileAuthProvider.PHONE, "+998901234567", null, "+998901234567", nowUtc);
        MobileAuthIdentity googleIdentity = MobileAuthIdentity.forCustomer(customer, MobileAuthProvider.GOOGLE, "sub123", "john.doe@example.com", null, nowUtc);

        when(identityRepository.findActiveForActor(ActorType.CUSTOMER, 100L)).thenReturn(List.of(phoneIdentity, googleIdentity));

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 100L);
        List<MobileAuthMethodResponse> methods = service.list(actor);

        assertThat(methods).hasSize(3);

        MobileAuthMethodResponse telegram = methods.stream().filter(m -> m.provider() == MobileAuthProvider.TELEGRAM).findFirst().orElseThrow();
        assertThat(telegram.linked()).isFalse();

        MobileAuthMethodResponse google = methods.stream().filter(m -> m.provider() == MobileAuthProvider.GOOGLE).findFirst().orElseThrow();
        assertThat(google.linked()).isTrue();
        assertThat(google.displayValue()).isEqualTo("j***e@example.com");

        MobileAuthMethodResponse phone = methods.stream().filter(m -> m.provider() == MobileAuthProvider.PHONE).findFirst().orElseThrow();
        assertThat(phone.linked()).isTrue();
        assertThat(phone.displayValue()).isEqualTo("+99890 *** ** 67");
    }

    @Test
    void givenValidGoogleTokenWhenLinkGoogleThenSavesIdentity() {
        Customer customer = new Customer("Customer", "+998901234567", LanguageCode.UZ, nowUtc);
        ReflectionTestUtils.setField(customer, "id", 100L);

        when(googleIdTokenVerifier.verify("valid-id-token", PushClientType.CUSTOMER_MOBILE))
                .thenReturn(new GoogleIdentity("sub123", "user@example.com", true, "User"));
        when(identityRepository.findActiveByProviderForUpdate(ActorType.CUSTOMER, MobileAuthProvider.GOOGLE, "sub123"))
                .thenReturn(Optional.empty());
        when(identityRepository.findActiveActorProviderForUpdate(ActorType.CUSTOMER, 100L, MobileAuthProvider.GOOGLE))
                .thenReturn(Optional.empty());
        when(customerRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(customer));

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 100L);
        service.linkGoogle(actor, new GoogleLinkRequest("valid-id-token"));

        verify(identityRepository).saveAndFlush(any(MobileAuthIdentity.class));
        assertThat(customer.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void givenMultipleAuthMethodsWhenUnlinkGoogleThenDeactivatesIdentityAndRevokesSessions() {
        Customer customer = Customer.google("Customer", "user@example.com", nowUtc, LanguageCode.UZ, nowUtc);
        ReflectionTestUtils.setField(customer, "id", 100L);

        MobileAuthIdentity phoneIdentity = MobileAuthIdentity.forCustomer(customer, MobileAuthProvider.PHONE, "+998901234567", null, "+998901234567", nowUtc);
        MobileAuthIdentity googleIdentity = MobileAuthIdentity.forCustomer(customer, MobileAuthProvider.GOOGLE, "sub123", "user@example.com", null, nowUtc);

        when(identityRepository.findActiveForActor(ActorType.CUSTOMER, 100L)).thenReturn(List.of(phoneIdentity, googleIdentity));
        when(identityRepository.findActiveActorProviderForUpdate(ActorType.CUSTOMER, 100L, MobileAuthProvider.GOOGLE)).thenReturn(Optional.of(googleIdentity));

        UUID sessionId = UUID.randomUUID();
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 100L, "customer:100", true, sessionId, PushClientType.CUSTOMER_MOBILE);
        service.unlinkGoogle(actor, sessionId);

        assertThat(googleIdentity.isActive()).isFalse();
        verify(refreshSessionService).revokeOtherFamiliesForActor(
                eq(sessionId),
                eq(ActorType.CUSTOMER),
                eq(100L),
                eq(MobileRefreshRevocationReason.AUTH_METHOD_UNLINKED));
    }

    @Test
    void givenOnlyGoogleAuthWhenUnlinkGoogleThenThrowsLastAuthMethod() {
        Customer customer = Customer.google("Customer", "user@example.com", nowUtc, LanguageCode.UZ, nowUtc);
        ReflectionTestUtils.setField(customer, "id", 100L);

        MobileAuthIdentity googleIdentity = MobileAuthIdentity.forCustomer(customer, MobileAuthProvider.GOOGLE, "sub123", "user@example.com", null, nowUtc);
        when(identityRepository.findActiveForActor(ActorType.CUSTOMER, 100L)).thenReturn(List.of(googleIdentity));

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 100L);
        assertThatThrownBy(() -> service.unlinkGoogle(actor, null))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.LAST_AUTH_METHOD);
    }
}
