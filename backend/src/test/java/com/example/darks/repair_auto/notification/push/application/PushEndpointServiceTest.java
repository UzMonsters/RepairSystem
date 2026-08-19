package com.example.darks.repair_auto.notification.push.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.notification.push.api.dto.PushEndpointRegisterRequest;
import com.example.darks.repair_auto.notification.push.api.dto.PushEndpointResponse;
import com.example.darks.repair_auto.notification.push.api.dto.PushEndpointUnregisterRequest;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.notification.push.domain.PushEndpoint;
import com.example.darks.repair_auto.notification.push.domain.PushFirebaseApp;
import com.example.darks.repair_auto.notification.push.domain.PushOwnerType;
import com.example.darks.repair_auto.notification.push.domain.PushPlatform;
import com.example.darks.repair_auto.notification.push.infrastructure.PushEndpointRepository;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

class PushEndpointServiceTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-18T10:00:00Z");

    private PushEndpointRepository repository;
    private UserRepository userRepository;
    private CustomerRepository customerRepository;
    private TechnicianRepository technicianRepository;
    private Clock clock;
    private PushEndpointService service;

    private User staffAdmin;
    private Customer customer;
    private Technician technician;

    @BeforeEach
    void setUp() {
        repository = mock(PushEndpointRepository.class);
        userRepository = mock(UserRepository.class);
        customerRepository = mock(CustomerRepository.class);
        technicianRepository = mock(TechnicianRepository.class);
        clock = Clock.fixed(Instant.parse("2026-08-18T10:00:00Z"), ZoneOffset.UTC);

        service = new PushEndpointService(repository, userRepository, customerRepository, technicianRepository, clock);

        staffAdmin = new User("Admin", "admin@example.com", "hash", UserRole.ADMIN, true, NOW);
        ReflectionTestUtils.setField(staffAdmin, "id", 1L);

        customer = new Customer("Ali Valiyev", "+998901234567", LanguageCode.UZ, NOW);
        ReflectionTestUtils.setField(customer, "id", 42L);

        technician = new Technician("Aziz Karimov", "+998901112233", "Cooling", "Notes", 5, LanguageCode.RU, true, NOW);
        ReflectionTestUtils.setField(technician, "id", 17L);
    }

    @Test
    void givenStaffUser_whenRegisterValidAdminWeb_thenCreatesNewPushEndpoint() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(staffAdmin));
        when(repository.findByFirebaseAppKeyAndFcmRegistrationToken(PushFirebaseApp.ADMIN_WEB, "fid-admin-123"))
                .thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(PushEndpoint.class))).thenAnswer(invocation -> {
            PushEndpoint pe = invocation.getArgument(0);
            ReflectionTestUtils.setField(pe, "id", 101L);
            return pe;
        });

        PushEndpointRegisterRequest request = new PushEndpointRegisterRequest(
                "fid-admin-123",
                PushClientType.ADMIN_WEB,
                PushPlatform.WEB,
                PushFirebaseApp.ADMIN_WEB,
                "1.0.0");

        PushEndpointResponse response = service.registerForStaff(1L, request);

        assertThat(response.id()).isEqualTo(101L);
        assertThat(response.clientType()).isEqualTo(PushClientType.ADMIN_WEB);
        assertThat(response.platform()).isEqualTo(PushPlatform.WEB);
        assertThat(response.firebaseAppKey()).isEqualTo(PushFirebaseApp.ADMIN_WEB);
        assertThat(response.enabled()).isTrue();
        assertThat(response.lastSeenAt()).isEqualTo(NOW);
    }

    @Test
    void givenExistingStaffEndpoint_whenRegisterAgain_thenTouchesEndpoint() {
        PushEndpoint existing = PushEndpoint.forStaff(
                staffAdmin,
                PushClientType.ADMIN_WEB,
                PushPlatform.WEB,
                PushFirebaseApp.ADMIN_WEB,
                "fid-admin-123",
                "1.0.0",
                NOW.minusDays(2));
        ReflectionTestUtils.setField(existing, "id", 101L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(staffAdmin));
        when(repository.findByFirebaseAppKeyAndFcmRegistrationToken(PushFirebaseApp.ADMIN_WEB, "fid-admin-123"))
                .thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(existing)).thenReturn(existing);

        PushEndpointRegisterRequest request = new PushEndpointRegisterRequest(
                "fid-admin-123",
                PushClientType.ADMIN_WEB,
                PushPlatform.WEB,
                PushFirebaseApp.ADMIN_WEB,
                "1.1.0");

        PushEndpointResponse response = service.registerForStaff(1L, request);

        assertThat(response.id()).isEqualTo(101L);
        assertThat(existing.getAppVersion()).isEqualTo("1.1.0");
        assertThat(existing.getLastSeenAt()).isEqualTo(NOW);
        assertThat(existing.isEnabled()).isTrue();
    }

    @Test
    void givenCustomerAndroid_whenRegister_thenCreatesCustomerPushEndpoint() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        when(customerRepository.findById(42L)).thenReturn(Optional.of(customer));
        when(repository.findByFirebaseAppKeyAndFcmRegistrationToken(PushFirebaseApp.CUSTOMER_ANDROID, "fid-cust-android"))
                .thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(PushEndpoint.class))).thenAnswer(invocation -> {
            PushEndpoint pe = invocation.getArgument(0);
            ReflectionTestUtils.setField(pe, "id", 201L);
            return pe;
        });

        PushEndpointRegisterRequest request = new PushEndpointRegisterRequest(
                "fid-cust-android",
                PushClientType.CUSTOMER_MOBILE,
                PushPlatform.ANDROID,
                PushFirebaseApp.CUSTOMER_ANDROID,
                "1.0.0");

        PushEndpointResponse response = service.registerForMobile(actor, request);

        assertThat(response.id()).isEqualTo(201L);
        assertThat(response.clientType()).isEqualTo(PushClientType.CUSTOMER_MOBILE);
        assertThat(response.platform()).isEqualTo(PushPlatform.ANDROID);
        assertThat(response.firebaseAppKey()).isEqualTo(PushFirebaseApp.CUSTOMER_ANDROID);
        assertThat(response.enabled()).isTrue();
    }

    @Test
    void givenCustomerIos_whenRegister_thenCreatesCustomerPushEndpoint() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        when(customerRepository.findById(42L)).thenReturn(Optional.of(customer));
        when(repository.findByFirebaseAppKeyAndFcmRegistrationToken(PushFirebaseApp.CUSTOMER_IOS, "fid-cust-ios"))
                .thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(PushEndpoint.class))).thenAnswer(invocation -> {
            PushEndpoint pe = invocation.getArgument(0);
            ReflectionTestUtils.setField(pe, "id", 202L);
            return pe;
        });

        PushEndpointRegisterRequest request = new PushEndpointRegisterRequest(
                "fid-cust-ios",
                PushClientType.CUSTOMER_MOBILE,
                PushPlatform.IOS,
                PushFirebaseApp.CUSTOMER_IOS,
                "1.0.0");

        PushEndpointResponse response = service.registerForMobile(actor, request);

        assertThat(response.id()).isEqualTo(202L);
        assertThat(response.platform()).isEqualTo(PushPlatform.IOS);
        assertThat(response.firebaseAppKey()).isEqualTo(PushFirebaseApp.CUSTOMER_IOS);
    }

    @Test
    void givenTechnicianAndroid_whenRegister_thenCreatesTechnicianPushEndpoint() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 17L, "+998901112233", true);
        when(technicianRepository.findById(17L)).thenReturn(Optional.of(technician));
        when(repository.findByFirebaseAppKeyAndFcmRegistrationToken(PushFirebaseApp.TECHNICIAN_ANDROID, "fid-tech-android"))
                .thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(PushEndpoint.class))).thenAnswer(invocation -> {
            PushEndpoint pe = invocation.getArgument(0);
            ReflectionTestUtils.setField(pe, "id", 301L);
            return pe;
        });

        PushEndpointRegisterRequest request = new PushEndpointRegisterRequest(
                "fid-tech-android",
                PushClientType.TECHNICIAN_MOBILE,
                PushPlatform.ANDROID,
                PushFirebaseApp.TECHNICIAN_ANDROID,
                "1.0.0");

        PushEndpointResponse response = service.registerForMobile(actor, request);

        assertThat(response.id()).isEqualTo(301L);
        assertThat(response.clientType()).isEqualTo(PushClientType.TECHNICIAN_MOBILE);
        assertThat(response.platform()).isEqualTo(PushPlatform.ANDROID);
        assertThat(response.firebaseAppKey()).isEqualTo(PushFirebaseApp.TECHNICIAN_ANDROID);
    }

    @Test
    void givenDeviceOwnedByCustomerA_whenCustomerBRegistersSameFid_thenTransfersOwnershipToCustomerB() {
        Customer customerB = new Customer("Bekzod", "+998909876543", LanguageCode.UZ, NOW);
        ReflectionTestUtils.setField(customerB, "id", 99L);

        PushEndpoint existing = PushEndpoint.forCustomer(
                customer,
                PushClientType.CUSTOMER_MOBILE,
                PushPlatform.ANDROID,
                PushFirebaseApp.CUSTOMER_ANDROID,
                "shared-device-fid",
                "1.0.0",
                NOW.minusDays(1));
        ReflectionTestUtils.setField(existing, "id", 401L);

        AuthenticatedMobileActor actorB = new AuthenticatedMobileActor(ActorType.CUSTOMER, 99L, "+998909876543", true);
        when(customerRepository.findById(99L)).thenReturn(Optional.of(customerB));
        when(repository.findByFirebaseAppKeyAndFcmRegistrationToken(PushFirebaseApp.CUSTOMER_ANDROID, "shared-device-fid"))
                .thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(existing)).thenReturn(existing);

        PushEndpointRegisterRequest request = new PushEndpointRegisterRequest(
                "shared-device-fid",
                PushClientType.CUSTOMER_MOBILE,
                PushPlatform.ANDROID,
                PushFirebaseApp.CUSTOMER_ANDROID,
                "1.0.1");

        PushEndpointResponse response = service.registerForMobile(actorB, request);

        assertThat(response.id()).isEqualTo(401L);
        assertThat(existing.isOwnedByCustomer(99L)).isTrue();
        assertThat(existing.isOwnedByCustomer(42L)).isFalse();
        assertThat(existing.getCustomer().getId()).isEqualTo(99L);
        assertThat(existing.isEnabled()).isTrue();
        assertThat(existing.getLastSeenAt()).isEqualTo(NOW);
    }

    @ParameterizedTest
    @CsvSource({
            "STAFF, CUSTOMER_MOBILE, ANDROID, CUSTOMER_ANDROID",
            "STAFF, ADMIN_WEB, ANDROID, ADMIN_WEB",
            "CUSTOMER, ADMIN_WEB, WEB, ADMIN_WEB",
            "CUSTOMER, CUSTOMER_MOBILE, WEB, ADMIN_WEB",
            "CUSTOMER, CUSTOMER_MOBILE, ANDROID, CUSTOMER_IOS",
            "CUSTOMER, TECHNICIAN_MOBILE, ANDROID, TECHNICIAN_ANDROID",
            "TECHNICIAN, CUSTOMER_MOBILE, ANDROID, CUSTOMER_ANDROID",
            "TECHNICIAN, TECHNICIAN_MOBILE, WEB, ADMIN_WEB",
            "TECHNICIAN, TECHNICIAN_MOBILE, ANDROID, TECHNICIAN_IOS"
    })
    void givenIncompatibleMatrix_whenValidateCompatibility_thenThrowsValidationError(
            PushOwnerType ownerType,
            PushClientType clientType,
            PushPlatform platform,
            PushFirebaseApp firebaseAppKey) {
        assertThatThrownBy(() -> service.validateCompatibility(ownerType, clientType, platform, firebaseAppKey))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void givenCurrentOwner_whenUnregisterStaff_thenDisablesPushEndpoint() {
        PushEndpoint existing = PushEndpoint.forStaff(
                staffAdmin,
                PushClientType.ADMIN_WEB,
                PushPlatform.WEB,
                PushFirebaseApp.ADMIN_WEB,
                "fid-admin-123",
                "1.0.0",
                NOW);
        assertThat(existing.isEnabled()).isTrue();

        when(repository.findByFirebaseAppKeyAndFcmRegistrationToken(PushFirebaseApp.ADMIN_WEB, "fid-admin-123"))
                .thenReturn(Optional.of(existing));

        service.unregisterForStaff(1L, new PushEndpointUnregisterRequest("fid-admin-123", PushFirebaseApp.ADMIN_WEB));

        assertThat(existing.isEnabled()).isFalse();
        verify(repository).saveAndFlush(existing);
    }

    @Test
    void givenNonOwner_whenUnregisterStaff_thenDoesNotDisableOrLeak() {
        PushEndpoint existing = PushEndpoint.forStaff(
                staffAdmin,
                PushClientType.ADMIN_WEB,
                PushPlatform.WEB,
                PushFirebaseApp.ADMIN_WEB,
                "fid-admin-123",
                "1.0.0",
                NOW);

        when(repository.findByFirebaseAppKeyAndFcmRegistrationToken(PushFirebaseApp.ADMIN_WEB, "fid-admin-123"))
                .thenReturn(Optional.of(existing));

        service.unregisterForStaff(999L, new PushEndpointUnregisterRequest("fid-admin-123", PushFirebaseApp.ADMIN_WEB));

        assertThat(existing.isEnabled()).isTrue();
    }

    @Test
    void givenCustomerOwner_whenUnregisterMobile_thenDisablesPushEndpoint() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        PushEndpoint existing = PushEndpoint.forCustomer(
                customer,
                PushClientType.CUSTOMER_MOBILE,
                PushPlatform.ANDROID,
                PushFirebaseApp.CUSTOMER_ANDROID,
                "fid-cust-android",
                "1.0.0",
                NOW);

        when(repository.findByFirebaseAppKeyAndFcmRegistrationToken(PushFirebaseApp.CUSTOMER_ANDROID, "fid-cust-android"))
                .thenReturn(Optional.of(existing));

        service.unregisterForMobile(actor, new PushEndpointUnregisterRequest("fid-cust-android", PushFirebaseApp.CUSTOMER_ANDROID));

        assertThat(existing.isEnabled()).isFalse();
        verify(repository).saveAndFlush(existing);
    }

    @Test
    void givenDisabledEndpoint_whenRegisterAgain_thenReEnablesAndUpdatesLastSeen() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        PushEndpoint existing = PushEndpoint.forCustomer(
                customer,
                PushClientType.CUSTOMER_MOBILE,
                PushPlatform.ANDROID,
                PushFirebaseApp.CUSTOMER_ANDROID,
                "fid-cust-android",
                "1.0.0",
                NOW.minusDays(5));
        existing.disable(NOW.minusDays(3));
        assertThat(existing.isEnabled()).isFalse();

        when(customerRepository.findById(42L)).thenReturn(Optional.of(customer));
        when(repository.findByFirebaseAppKeyAndFcmRegistrationToken(PushFirebaseApp.CUSTOMER_ANDROID, "fid-cust-android"))
                .thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(existing)).thenReturn(existing);

        PushEndpointRegisterRequest request = new PushEndpointRegisterRequest(
                "fid-cust-android",
                PushClientType.CUSTOMER_MOBILE,
                PushPlatform.ANDROID,
                PushFirebaseApp.CUSTOMER_ANDROID,
                "1.0.2");

        PushEndpointResponse response = service.registerForMobile(actor, request);

        assertThat(response.enabled()).isTrue();
        assertThat(existing.isEnabled()).isTrue();
        assertThat(existing.getLastSeenAt()).isEqualTo(NOW);
    }

    @Test
    void givenEnabledEndpoints_whenFindDeliveryEndpoints_thenReturnsOnlyEnabled() {
        PushEndpoint pe1 = PushEndpoint.forCustomer(customer, PushClientType.CUSTOMER_MOBILE, PushPlatform.ANDROID, PushFirebaseApp.CUSTOMER_ANDROID, "fid-1", null, NOW);
        PushEndpoint pe2 = PushEndpoint.forCustomer(customer, PushClientType.CUSTOMER_MOBILE, PushPlatform.IOS, PushFirebaseApp.CUSTOMER_IOS, "fid-2", null, NOW);

        when(repository.findByCustomerIdAndEnabledTrue(42L)).thenReturn(List.of(pe1, pe2));

        List<PushEndpoint> found = service.findEnabledForCustomer(42L);

        assertThat(found).containsExactly(pe1, pe2);
    }

    @Test
    void givenPermanentFailure_whenDisableInvalidEndpoint_thenDisablesEndpoint() {
        PushEndpoint pe = PushEndpoint.forCustomer(customer, PushClientType.CUSTOMER_MOBILE, PushPlatform.ANDROID, PushFirebaseApp.CUSTOMER_ANDROID, "fid-1", null, NOW);
        ReflectionTestUtils.setField(pe, "id", 777L);
        assertThat(pe.isEnabled()).isTrue();

        when(repository.findById(777L)).thenReturn(Optional.of(pe));

        service.disableInvalidEndpoint(777L);

        assertThat(pe.isEnabled()).isFalse();
        verify(repository).saveAndFlush(pe);
    }
}
