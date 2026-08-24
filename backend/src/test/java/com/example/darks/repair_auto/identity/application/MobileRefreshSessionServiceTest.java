package com.example.darks.repair_auto.identity.application;

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
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.MobileAuthProvider;
import com.example.darks.repair_auto.identity.domain.MobileRefreshRevocationReason;
import com.example.darks.repair_auto.identity.domain.MobileRefreshSession;
import com.example.darks.repair_auto.identity.domain.MobileSession;
import com.example.darks.repair_auto.identity.infrastructure.persistence.MobileRefreshSessionRepository;
import com.example.darks.repair_auto.notification.push.domain.PushPlatform;
import com.example.darks.repair_auto.shared.config.AppProperties;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MobileRefreshSessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-18T10:00:00Z");

    private MobileRefreshSessionRepository repository;
    private CustomerRepository customerRepository;
    private TechnicianRepository technicianRepository;
    private RefreshTokenGenerator tokenGenerator;
    private TokenHashService tokenHashService;
    private AppProperties properties;
    private com.example.darks.repair_auto.notification.push.application.PushEndpointService pushEndpointService;
    private MobileSessionService mobileSessionService;
    private MobileRefreshSessionService service;

    @BeforeEach
    void setUp() {
        repository = mock(MobileRefreshSessionRepository.class);
        customerRepository = mock(CustomerRepository.class);
        technicianRepository = mock(TechnicianRepository.class);
        tokenGenerator = mock(RefreshTokenGenerator.class);
        pushEndpointService = mock(com.example.darks.repair_auto.notification.push.application.PushEndpointService.class);
        mobileSessionService = mock(MobileSessionService.class);
        tokenHashService = new TokenHashService();
        properties = new AppProperties(
                new AppProperties.Cors(List.of(), List.of(), List.of(), List.of(), false),
                new AppProperties.Trace("X-Trace-Id"),
                new AppProperties.Jwt("test-secret-at-least-32-characters-long", "repair-auto", Duration.ofMinutes(15)),
                Duration.ofDays(1),
                Duration.ofDays(7),
                Duration.ofDays(30),
                new AppProperties.BootstrapAdmin(false, "", "", ""));

        service = new MobileRefreshSessionService(
                repository,
                customerRepository,
                technicianRepository,
                tokenGenerator,
                tokenHashService,
                properties,
                pushEndpointService,
                mobileSessionService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void givenCustomerWhenCreateInitialThenSessionSavedWithFamilyIdAndHash() {
        Customer customer = new Customer("Customer 1", "+998901234567", LanguageCode.UZ, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(customer, "id", 100L);
        MobileSession customerSession = MobileSession.forCustomer(
                customer,
                MobileAuthProvider.PHONE,
                PushPlatform.ANDROID,
                null, null, null, "127.0.0.1", "test-agent",
                OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(NOW.plus(Duration.ofDays(30)), ZoneOffset.UTC));

        when(tokenGenerator.generate()).thenReturn("raw-refresh-token-123");
        when(repository.save(any(MobileRefreshSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MobileRefreshSessionService.IssuedMobileRefreshToken result = service.createForCustomer(customer, customerSession);

        assertThat(result.rawToken()).isEqualTo("raw-refresh-token-123");
        assertThat(result.session().getActorType()).isEqualTo(ActorType.CUSTOMER);
        assertThat(result.session().getCustomer()).isEqualTo(customer);
        assertThat(result.session().getTechnician()).isNull();
        assertThat(result.session().getMobileSession()).isEqualTo(customerSession);
        assertThat(result.session().getTokenFamilyId()).isNotNull();
        assertThat(result.session().getTokenHash()).isEqualTo(tokenHashService.hash("raw-refresh-token-123"));
        assertThat(result.session().getExpiresAt()).isEqualTo(OffsetDateTime.ofInstant(NOW.plus(Duration.ofDays(30)), ZoneOffset.UTC));
    }

    @Test
    void givenTechnicianWhenCreateInitialThenSessionSavedWithFamilyIdAndHash() {
        Technician technician = new Technician("Tech 1", "+998909876543", "Washer", "Notes", 5, LanguageCode.RU, true, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(technician, "id", 200L);
        MobileSession techSession = MobileSession.forTechnician(
                technician,
                MobileAuthProvider.PHONE,
                PushPlatform.ANDROID,
                null, null, null, "127.0.0.1", "test-agent",
                OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(NOW.plus(Duration.ofDays(30)), ZoneOffset.UTC));

        when(tokenGenerator.generate()).thenReturn("raw-refresh-token-456");
        when(repository.save(any(MobileRefreshSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MobileRefreshSessionService.IssuedMobileRefreshToken result = service.createForTechnician(technician, techSession);

        assertThat(result.rawToken()).isEqualTo("raw-refresh-token-456");
        assertThat(result.session().getActorType()).isEqualTo(ActorType.TECHNICIAN);
        assertThat(result.session().getTechnician()).isEqualTo(technician);
        assertThat(result.session().getCustomer()).isNull();
        assertThat(result.session().getMobileSession()).isEqualTo(techSession);
        assertThat(result.session().getTokenFamilyId()).isNotNull();
        assertThat(result.session().getTokenHash()).isEqualTo(tokenHashService.hash("raw-refresh-token-456"));
    }

    @Test
    void givenValidCustomerRefreshTokenWhenRotateThenOldSessionRotatedAndNewTokenPairIssued() {
        Customer customer = new Customer("Customer 1", "+998901234567", LanguageCode.UZ, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(customer, "id", 100L);

        MobileSession customerSession = MobileSession.forCustomer(
                customer,
                MobileAuthProvider.PHONE,
                PushPlatform.ANDROID,
                null, null, null, "127.0.0.1", "test-agent",
                OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(NOW.plus(Duration.ofDays(30)), ZoneOffset.UTC));
        ReflectionTestUtils.setField(customerSession, "id", UUID.randomUUID());

        UUID familyId = UUID.randomUUID();
        String oldRawToken = "old-raw-token";
        String oldHash = tokenHashService.hash(oldRawToken);

        MobileRefreshSession oldSession = MobileRefreshSession.forCustomer(
                customer,
                oldHash,
                familyId,
                null,
                customerSession,
                OffsetDateTime.ofInstant(NOW.minus(Duration.ofDays(1)), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(NOW.plus(Duration.ofDays(29)), ZoneOffset.UTC));
        ReflectionTestUtils.setField(oldSession, "id", 1L);

        when(repository.findByTokenHashForUpdate(oldHash)).thenReturn(Optional.of(oldSession));
        when(customerRepository.findById(100L)).thenReturn(Optional.of(customer));
        when(tokenGenerator.generate()).thenReturn("new-raw-token");
        when(repository.save(any(MobileRefreshSession.class))).thenAnswer(invocation -> {
            MobileRefreshSession session = invocation.getArgument(0);
            ReflectionTestUtils.setField(session, "id", 2L);
            return session;
        });

        MobileRefreshSessionService.MobileRotationResult rotation = service.rotate(oldRawToken);

        assertThat(rotation.actorType()).isEqualTo(ActorType.CUSTOMER);
        assertThat(rotation.actorId()).isEqualTo(100L);
        assertThat(rotation.phone()).isEqualTo("+998901234567");
        assertThat(rotation.rawRefreshToken()).isEqualTo("new-raw-token");
        assertThat(rotation.session().getTokenFamilyId()).isEqualTo(familyId);
        assertThat(rotation.session().getParentSessionId()).isEqualTo(1L);

        assertThat(oldSession.isUsed()).isTrue();
        assertThat(oldSession.isRevoked()).isTrue();
        assertThat(oldSession.getRevocationReason()).isEqualTo(MobileRefreshRevocationReason.ROTATED.name());
        assertThat(oldSession.getReplacedBySessionId()).isEqualTo(2L);
    }

    @Test
    void givenValidTechnicianRefreshTokenWhenRotateThenOldSessionRotatedAndNewTokenPairIssued() {
        Technician technician = new Technician("Tech 1", "+998909876543", "Washer", "Notes", 5, LanguageCode.RU, true, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(technician, "id", 200L);

        MobileSession techSession = MobileSession.forTechnician(
                technician,
                MobileAuthProvider.PHONE,
                PushPlatform.ANDROID,
                null, null, null, "127.0.0.1", "test-agent",
                OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(NOW.plus(Duration.ofDays(30)), ZoneOffset.UTC));
        ReflectionTestUtils.setField(techSession, "id", UUID.randomUUID());

        UUID familyId = UUID.randomUUID();
        String oldRawToken = "tech-old-token";
        String oldHash = tokenHashService.hash(oldRawToken);

        MobileRefreshSession oldSession = MobileRefreshSession.forTechnician(
                technician,
                oldHash,
                familyId,
                null,
                techSession,
                OffsetDateTime.ofInstant(NOW.minus(Duration.ofDays(1)), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(NOW.plus(Duration.ofDays(29)), ZoneOffset.UTC));
        ReflectionTestUtils.setField(oldSession, "id", 10L);

        when(repository.findByTokenHashForUpdate(oldHash)).thenReturn(Optional.of(oldSession));
        when(technicianRepository.findById(200L)).thenReturn(Optional.of(technician));
        when(tokenGenerator.generate()).thenReturn("tech-new-token");
        when(repository.save(any(MobileRefreshSession.class))).thenAnswer(invocation -> {
            MobileRefreshSession session = invocation.getArgument(0);
            ReflectionTestUtils.setField(session, "id", 11L);
            return session;
        });

        MobileRefreshSessionService.MobileRotationResult rotation = service.rotate(oldRawToken);

        assertThat(rotation.actorType()).isEqualTo(ActorType.TECHNICIAN);
        assertThat(rotation.actorId()).isEqualTo(200L);
        assertThat(rotation.phone()).isEqualTo("+998909876543");
        assertThat(rotation.rawRefreshToken()).isEqualTo("tech-new-token");
        assertThat(rotation.session().getTokenFamilyId()).isEqualTo(familyId);
        assertThat(rotation.session().getParentSessionId()).isEqualTo(10L);
    }

    @Test
    void givenAlreadyUsedTokenWhenRotateThenReuseDetectedAndFamilyRevoked() {
        Customer customer = new Customer("Customer 1", "+998901234567", LanguageCode.UZ, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(customer, "id", 100L);

        UUID familyId = UUID.randomUUID();
        String rawToken = "already-used-token";
        String tokenHash = tokenHashService.hash(rawToken);

        MobileRefreshSession usedSession = MobileRefreshSession.forCustomer(
                customer,
                tokenHash,
                familyId,
                null,
                OffsetDateTime.ofInstant(NOW.minus(Duration.ofDays(2)), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(NOW.plus(Duration.ofDays(28)), ZoneOffset.UTC));
        usedSession.markUsed(OffsetDateTime.ofInstant(NOW.minus(Duration.ofDays(1)), ZoneOffset.UTC));

        when(repository.findByTokenHashForUpdate(tokenHash)).thenReturn(Optional.of(usedSession));

        BusinessException exception = catchThrowableOfType(
                () -> service.rotate(rawToken),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MOBILE_REFRESH_TOKEN_REUSED);
        verify(repository).revokeAllForCustomer(eq(100L), any(), eq(MobileRefreshRevocationReason.REUSE_DETECTED.name()));
        verify(pushEndpointService).disableAllForCustomer(100L);
        verify(repository, never()).save(any());
    }

    @Test
    void givenExpiredTokenWhenRotateThenThrowsMobileRefreshTokenExpired() {
        Customer customer = new Customer("Customer 1", "+998901234567", LanguageCode.UZ, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(customer, "id", 100L);

        UUID familyId = UUID.randomUUID();
        String rawToken = "expired-token";
        String tokenHash = tokenHashService.hash(rawToken);

        MobileRefreshSession expiredSession = MobileRefreshSession.forCustomer(
                customer,
                tokenHash,
                familyId,
                null,
                OffsetDateTime.ofInstant(NOW.minus(Duration.ofDays(35)), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(NOW.minus(Duration.ofDays(5)), ZoneOffset.UTC));

        when(repository.findByTokenHashForUpdate(tokenHash)).thenReturn(Optional.of(expiredSession));

        BusinessException exception = catchThrowableOfType(
                () -> service.rotate(rawToken),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MOBILE_REFRESH_TOKEN_EXPIRED);
    }

    @Test
    void givenInactiveCustomerWhenRotateThenThrowsAccountInactiveAndRevokesFamily() {
        Customer customer = new Customer("Customer 1", "+998901234567", LanguageCode.UZ, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        customer.setActive(false, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(customer, "id", 100L);

        MobileSession customerSession = MobileSession.forCustomer(
                customer,
                MobileAuthProvider.PHONE,
                PushPlatform.ANDROID,
                null, null, null, "127.0.0.1", "test-agent",
                OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(NOW.plus(Duration.ofDays(30)), ZoneOffset.UTC));
        ReflectionTestUtils.setField(customerSession, "id", UUID.randomUUID());

        UUID familyId = UUID.randomUUID();
        String rawToken = "inactive-customer-token";
        String tokenHash = tokenHashService.hash(rawToken);

        MobileRefreshSession session = MobileRefreshSession.forCustomer(
                customer,
                tokenHash,
                familyId,
                null,
                customerSession,
                OffsetDateTime.ofInstant(NOW.minus(Duration.ofDays(1)), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(NOW.plus(Duration.ofDays(29)), ZoneOffset.UTC));

        when(repository.findByTokenHashForUpdate(tokenHash)).thenReturn(Optional.of(session));
        when(customerRepository.findById(100L)).thenReturn(Optional.of(customer));

        BusinessException exception = catchThrowableOfType(
                () -> service.rotate(rawToken),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_INACTIVE);
        verify(repository).revokeFamily(eq(familyId), any(), eq(MobileRefreshRevocationReason.ACCOUNT_INACTIVE.name()));
    }

    @Test
    void givenBlankOrNullTokenWhenRotateThenThrowsMobileRefreshTokenInvalid() {
        assertThat(catchThrowableOfType(() -> service.rotate(null), BusinessException.class).getErrorCode())
                .isEqualTo(ErrorCode.MOBILE_REFRESH_TOKEN_INVALID);
        assertThat(catchThrowableOfType(() -> service.rotate("   "), BusinessException.class).getErrorCode())
                .isEqualTo(ErrorCode.MOBILE_REFRESH_TOKEN_INVALID);
    }

    @Test
    void givenUnknownTokenWhenRotateThenThrowsMobileRefreshTokenInvalid() {
        when(repository.findByTokenHashForUpdate(any())).thenReturn(Optional.empty());

        BusinessException exception = catchThrowableOfType(
                () -> service.rotate("unknown-token"),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MOBILE_REFRESH_TOKEN_INVALID);
    }

    @Test
    void givenRawTokenWhenRevokeByRawTokenThenRevokesWholeFamily() {
        Customer customer = new Customer("Customer 1", "+998901234567", LanguageCode.UZ, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        UUID familyId = UUID.randomUUID();
        String rawToken = "logout-token";
        String tokenHash = tokenHashService.hash(rawToken);

        MobileRefreshSession session = MobileRefreshSession.forCustomer(
                customer,
                tokenHash,
                familyId,
                null,
                OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(NOW.plus(Duration.ofDays(30)), ZoneOffset.UTC));

        when(repository.findByTokenHashForUpdate(tokenHash)).thenReturn(Optional.of(session));

        service.revokeByRawToken(rawToken);

        verify(repository).revokeFamily(eq(familyId), any(), eq(MobileRefreshRevocationReason.LOGOUT.name()));
    }

    @Test
    void givenCustomerIdWhenRevokeAllForCustomerThenRevokesAllActiveSessions() {
        service.revokeAllForCustomer(42L, MobileRefreshRevocationReason.LOGOUT_ALL);

        verify(repository).revokeAllForCustomer(eq(42L), any(), eq(MobileRefreshRevocationReason.LOGOUT_ALL.name()));
    }

    @Test
    void givenTechnicianIdWhenRevokeAllForTechnicianThenRevokesAllActiveSessions() {
        service.revokeAllForTechnician(17L, MobileRefreshRevocationReason.TELEGRAM_IDENTITY_CHANGED);

        verify(repository).revokeAllForTechnician(eq(17L), any(), eq(MobileRefreshRevocationReason.TELEGRAM_IDENTITY_CHANGED.name()));
    }
}
