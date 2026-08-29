package com.example.darks.repair_auto.identity.mobile.telegram;

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
import com.example.darks.repair_auto.identity.application.ActorAccessLifecycleService;
import com.example.darks.repair_auto.identity.application.MobileRefreshSessionService;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.MobileRefreshSession;
import com.example.darks.repair_auto.identity.domain.MobileSession;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.infrastructure.security.JwtTokenService;
import com.example.darks.repair_auto.identity.mobile.telegram.dto.MobileAuthResponse;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TelegramMobileAuthServiceTest {

    private TelegramIdTokenVerifier idTokenVerifier;
    private CustomerRepository customerRepository;
    private TechnicianRepository technicianRepository;
    private JwtTokenService jwtTokenService;
    private MobileRefreshSessionService mobileRefreshSessionService;
    private ActorAccessLifecycleService actorAccessLifecycleService;
    private TelegramMobileAuthService service;

    @BeforeEach
    void setUp() {
        idTokenVerifier = mock(TelegramIdTokenVerifier.class);
        customerRepository = mock(CustomerRepository.class);
        technicianRepository = mock(TechnicianRepository.class);
        jwtTokenService = mock(JwtTokenService.class);
        mobileRefreshSessionService = mock(MobileRefreshSessionService.class);
        actorAccessLifecycleService = mock(ActorAccessLifecycleService.class);

        service = new TelegramMobileAuthService(
                idTokenVerifier,
                customerRepository,
                technicianRepository,
                jwtTokenService,
                mobileRefreshSessionService,
                actorAccessLifecycleService,
                null);
    }

    @Test
    void givenValidCustomerTokenAndActiveLinkedCustomerWhenLoginCustomerThenReturnsRepairAutoTokens() {
        TelegramIdentity identity = new TelegramIdentity(112233L, "112233", "Ali Valiyev", "alivaliyev", "+998901234567", true);
        when(idTokenVerifier.verifyCustomerToken("valid-customer-token")).thenReturn(identity);

        Customer customer = new Customer("Ali Valiyev", "+998901234567", LanguageCode.UZ, OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(customer, "id", 42L);
        customer.linkTelegram(112233L, 998877L, LanguageCode.UZ, OffsetDateTime.now(ZoneOffset.UTC));

        MobileRefreshSession session = mock(MobileRefreshSession.class);
        when(customerRepository.findByTelegramUserId(112233L)).thenReturn(Optional.of(customer));
        when(jwtTokenService.issueMobile(eq(ActorType.CUSTOMER), eq(42L), eq(0L), any(UUID.class), eq(PushClientType.CUSTOMER_MOBILE), eq("+998901234567")))
                .thenReturn("repair-auto-jwt-cust-42");
        when(jwtTokenService.accessTokenTtlSeconds()).thenReturn(900L);
        when(mobileRefreshSessionService.createForCustomer(eq(customer), any(MobileSession.class)))
                .thenReturn(new MobileRefreshSessionService.IssuedMobileRefreshToken("raw-refresh-cust-42", session));
        when(mobileRefreshSessionService.remainingTtlSeconds(session)).thenReturn(2592000L);

        MobileAuthResponse response = service.loginCustomer("valid-customer-token");

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isEqualTo("repair-auto-jwt-cust-42");
        assertThat(response.refreshToken()).isEqualTo("raw-refresh-cust-42");
        assertThat(response.expiresIn()).isEqualTo(900L);
        assertThat(response.refreshExpiresIn()).isEqualTo(2592000L);
        assertThat(response.actor().type()).isEqualTo(ActorType.CUSTOMER);
        assertThat(response.actor().id()).isEqualTo(42L);
        assertThat(response.actor().fullName()).isEqualTo("Ali Valiyev");
        assertThat(response.actor().phone()).isEqualTo("+998901234567");
        assertThat(response.actor().preferredLanguage()).isEqualTo("uz");

        verify(customerRepository).findByTelegramUserId(112233L);
        verify(technicianRepository, never()).findByTelegramUserId(any());
        verify(jwtTokenService).issueMobile(eq(ActorType.CUSTOMER), eq(42L), eq(0L), any(UUID.class), eq(PushClientType.CUSTOMER_MOBILE), eq("+998901234567"));
        verify(mobileRefreshSessionService).createForCustomer(eq(customer), any(MobileSession.class));
    }

    @Test
    void givenValidCustomerTokenAndUnlinkedCustomerWhenLoginCustomerThenThrowsTelegramAccountNotLinked() {
        TelegramIdentity identity = new TelegramIdentity(999999L, "999999", "Unknown", null, null, false);
        when(idTokenVerifier.verifyCustomerToken("unlinked-customer-token")).thenReturn(identity);
        when(customerRepository.findByTelegramUserId(999999L)).thenReturn(Optional.empty());

        BusinessException exception = catchThrowableOfType(
                () -> service.loginCustomer("unlinked-customer-token"),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TELEGRAM_ACCOUNT_NOT_LINKED);
        verify(customerRepository).findByTelegramUserId(999999L);
        verify(technicianRepository, never()).findByTelegramUserId(any());
        verify(jwtTokenService, never()).issueMobile(any(), any(), any(), any(), any(), any());
        verify(mobileRefreshSessionService, never()).createForCustomer(any(), any());
    }

    @Test
    void givenValidCustomerTokenAndInactiveCustomerWhenLoginCustomerThenThrowsAccountInactive() {
        TelegramIdentity identity = new TelegramIdentity(112233L, "112233", "Inactive Customer", null, null, false);
        when(idTokenVerifier.verifyCustomerToken("inactive-customer-token")).thenReturn(identity);

        Customer customer = new Customer("Inactive Customer", "+998901234567", LanguageCode.UZ, OffsetDateTime.now(ZoneOffset.UTC));
        customer.setActive(false, OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(customer, "id", 42L);

        when(customerRepository.findByTelegramUserId(112233L)).thenReturn(Optional.of(customer));

        BusinessException exception = catchThrowableOfType(
                () -> service.loginCustomer("inactive-customer-token"),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_INACTIVE);
        verify(jwtTokenService, never()).issueMobile(any(), any(), any(), any(), any(), any());
        verify(mobileRefreshSessionService, never()).createForCustomer(any(), any());
    }

    @Test
    void givenValidTechnicianTokenAndActiveLinkedTechnicianWhenLoginTechnicianThenReturnsRepairAutoTokens() {
        TelegramIdentity identity = new TelegramIdentity(445566L, "445566", "Vali Master", "valimaster", "+998909876543", true);
        when(idTokenVerifier.verifyTechnicianToken("valid-tech-token")).thenReturn(identity);

        Technician technician = new Technician("Vali Master", "+998909876543", "Diagnostics", "Notes", 5, LanguageCode.RU, true, OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(technician, "id", 17L);
        technician.linkTelegram(445566L, 112233L, OffsetDateTime.now(ZoneOffset.UTC));

        MobileRefreshSession session = mock(MobileRefreshSession.class);
        when(technicianRepository.findByTelegramUserId(445566L)).thenReturn(Optional.of(technician));
        when(jwtTokenService.issueMobile(eq(ActorType.TECHNICIAN), eq(17L), eq(0L), any(UUID.class), eq(PushClientType.TECHNICIAN_MOBILE), eq("+998909876543")))
                .thenReturn("repair-auto-jwt-tech-17");
        when(jwtTokenService.accessTokenTtlSeconds()).thenReturn(900L);
        when(mobileRefreshSessionService.createForTechnician(eq(technician), any(MobileSession.class)))
                .thenReturn(new MobileRefreshSessionService.IssuedMobileRefreshToken("raw-refresh-tech-17", session));
        when(mobileRefreshSessionService.remainingTtlSeconds(session)).thenReturn(2592000L);

        MobileAuthResponse response = service.loginTechnician("valid-tech-token");

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isEqualTo("repair-auto-jwt-tech-17");
        assertThat(response.refreshToken()).isEqualTo("raw-refresh-tech-17");
        assertThat(response.expiresIn()).isEqualTo(900L);
        assertThat(response.refreshExpiresIn()).isEqualTo(2592000L);
        assertThat(response.actor().type()).isEqualTo(ActorType.TECHNICIAN);
        assertThat(response.actor().id()).isEqualTo(17L);
        assertThat(response.actor().fullName()).isEqualTo("Vali Master");
        assertThat(response.actor().phone()).isEqualTo("+998909876543");
        assertThat(response.actor().preferredLanguage()).isEqualTo("ru");

        verify(technicianRepository).findByTelegramUserId(445566L);
        verify(customerRepository, never()).findByTelegramUserId(any());
        verify(jwtTokenService).issueMobile(eq(ActorType.TECHNICIAN), eq(17L), eq(0L), any(UUID.class), eq(PushClientType.TECHNICIAN_MOBILE), eq("+998909876543"));
        verify(mobileRefreshSessionService).createForTechnician(eq(technician), any(MobileSession.class));
    }

    @Test
    void givenValidTechnicianTokenAndUnlinkedTechnicianWhenLoginTechnicianThenThrowsTelegramAccountNotLinked() {
        TelegramIdentity identity = new TelegramIdentity(888888L, "888888", "Unknown Tech", null, null, false);
        when(idTokenVerifier.verifyTechnicianToken("unlinked-tech-token")).thenReturn(identity);
        when(technicianRepository.findByTelegramUserId(888888L)).thenReturn(Optional.empty());

        BusinessException exception = catchThrowableOfType(
                () -> service.loginTechnician("unlinked-tech-token"),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TELEGRAM_ACCOUNT_NOT_LINKED);
        verify(technicianRepository).findByTelegramUserId(888888L);
        verify(customerRepository, never()).findByTelegramUserId(any());
        verify(jwtTokenService, never()).issueMobile(any(), any(), any(), any(), any(), any());
    }

    @Test
    void givenValidTechnicianTokenAndInactiveTechnicianWhenLoginTechnicianThenThrowsAccountInactive() {
        TelegramIdentity identity = new TelegramIdentity(445566L, "445566", "Inactive Tech", null, null, false);
        when(idTokenVerifier.verifyTechnicianToken("inactive-tech-token")).thenReturn(identity);

        Technician technician = new Technician("Inactive Tech", "+998909876543", "Diagnostics", "Notes", 5, LanguageCode.RU, false, OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(technician, "id", 17L);

        when(technicianRepository.findByTelegramUserId(445566L)).thenReturn(Optional.of(technician));

        BusinessException exception = catchThrowableOfType(
                () -> service.loginTechnician("inactive-tech-token"),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_INACTIVE);
        verify(jwtTokenService, never()).issueMobile(any(), any(), any(), any(), any(), any());
    }

    @Test
    void givenValidRefreshTokenWhenRefreshThenReturnsNewTokens() {
        Customer customer = new Customer("Customer 1", "+998901234567", LanguageCode.UZ, OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(customer, "id", 100L);
        ReflectionTestUtils.setField(customer, "authVersion", 0L);
        MobileRefreshSession replacement = mock(MobileRefreshSession.class);
        MobileSession mobileSession = mock(MobileSession.class);
        UUID sessionId = UUID.randomUUID();
        when(mobileSession.getId()).thenReturn(sessionId);
        when(mobileSession.getClientType()).thenReturn(PushClientType.CUSTOMER_MOBILE);
        when(replacement.getMobileSession()).thenReturn(mobileSession);

        MobileRefreshSessionService.MobileRotationResult rotation = new MobileRefreshSessionService.MobileRotationResult(
                ActorType.CUSTOMER,
                customer,
                null,
                "replacement-refresh-token",
                replacement);

        when(mobileRefreshSessionService.rotate("valid-raw-refresh-token")).thenReturn(rotation);
        when(jwtTokenService.issueMobile(ActorType.CUSTOMER, 100L, 0L, sessionId, PushClientType.CUSTOMER_MOBILE, "+998901234567"))
                .thenReturn("new-access-jwt");
        when(jwtTokenService.accessTokenTtlSeconds()).thenReturn(900L);
        when(mobileRefreshSessionService.remainingTtlSeconds(replacement)).thenReturn(2592000L);

        MobileAuthResponse response = service.refresh("valid-raw-refresh-token");

        assertThat(response.accessToken()).isEqualTo("new-access-jwt");
        assertThat(response.refreshToken()).isEqualTo("replacement-refresh-token");
        assertThat(response.actor().id()).isEqualTo(100L);
        assertThat(response.actor().type()).isEqualTo(ActorType.CUSTOMER);
        assertThat(response.session().id()).isEqualTo(sessionId);
    }

    @Test
    void givenRefreshTokenWhenLogoutThenRevokesSession() {
        service.logout("raw-token-to-logout");

        verify(mobileRefreshSessionService).revokeByRawToken("raw-token-to-logout");
    }

    @Test
    void givenCustomerPrincipalWhenLogoutAllThenRevokesAllForCustomer() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 55L, "+998901112233", true);

        service.logoutAll(actor);

        verify(actorAccessLifecycleService).onCustomerLogoutAll(55L);
    }

    @Test
    void givenTechnicianPrincipalWhenLogoutAllThenRevokesAllForTechnician() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 77L, "+998904445566", true);

        service.logoutAll(actor);

        verify(actorAccessLifecycleService).onTechnicianLogoutAll(77L);
    }
}
