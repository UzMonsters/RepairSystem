package com.example.darks.repair_auto.identity.mobile.profile.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.mobile.profile.api.dto.MobileProfilePatchRequest;
import com.example.darks.repair_auto.identity.mobile.profile.api.dto.MobileProfileResponse;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MobileProfileServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-18T10:00:00Z");

    private CustomerRepository customerRepository;
    private TechnicianRepository technicianRepository;
    private MobileProfileService service;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        technicianRepository = mock(TechnicianRepository.class);
        service = new MobileProfileService(customerRepository, technicianRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void givenCustomerActorWhenGetProfileThenReturnsCustomerProfile() {
        Customer customer = new Customer("Ali Valiyev", "+998901234567", LanguageCode.UZ, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(customer, "id", 42L);
        customer.linkTelegram(112233L, 998877L, LanguageCode.UZ, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));

        when(customerRepository.findById(42L)).thenReturn(Optional.of(customer));

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        MobileProfileResponse response = service.getProfile(actor);

        assertThat(response.actorType()).isEqualTo(ActorType.CUSTOMER);
        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.fullName()).isEqualTo("Ali Valiyev");
        assertThat(response.phone()).isEqualTo("+998901234567");
        assertThat(response.preferredLanguage()).isEqualTo("uz");
        assertThat(response.telegramLinked()).isTrue();
        assertThat(response.technician()).isNull();
    }

    @Test
    void givenTechnicianActorWhenGetProfileThenReturnsTechnicianProfileWithMetadata() {
        Technician technician = new Technician("Aziz Karimov", "+998901112233", "Washer", "Notes", 5, LanguageCode.RU, true, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(technician, "id", 17L);
        technician.linkTelegram(445566L, 112233L, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));

        when(technicianRepository.findById(17L)).thenReturn(Optional.of(technician));

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 17L, "+998901112233", true);
        MobileProfileResponse response = service.getProfile(actor);

        assertThat(response.actorType()).isEqualTo(ActorType.TECHNICIAN);
        assertThat(response.id()).isEqualTo(17L);
        assertThat(response.fullName()).isEqualTo("Aziz Karimov");
        assertThat(response.phone()).isEqualTo("+998901112233");
        assertThat(response.preferredLanguage()).isEqualTo("ru");
        assertThat(response.telegramLinked()).isTrue();
        assertThat(response.technician()).isNotNull();
        assertThat(response.technician().specialization()).isEqualTo("Washer");
        assertThat(response.technician().maxActiveJobs()).isEqualTo(5);
        assertThat(response.technician().active()).isTrue();
    }

    @Test
    void givenInactiveCustomerWhenGetProfileThenThrowsAccountInactive() {
        Customer customer = new Customer("Inactive Customer", "+998901234567", LanguageCode.UZ, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        customer.setActive(false, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(customer, "id", 42L);

        when(customerRepository.findById(42L)).thenReturn(Optional.of(customer));

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);

        BusinessException exception = catchThrowableOfType(
                () -> service.getProfile(actor),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_INACTIVE);
    }

    @Test
    void givenInactiveTechnicianWhenGetProfileThenThrowsAccountInactive() {
        Technician technician = new Technician("Inactive Tech", "+998901112233", "Washer", "Notes", 5, LanguageCode.RU, false, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(technician, "id", 17L);

        when(technicianRepository.findById(17L)).thenReturn(Optional.of(technician));

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 17L, "+998901112233", true);

        BusinessException exception = catchThrowableOfType(
                () -> service.getProfile(actor),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_INACTIVE);
    }

    @Test
    void givenCustomerActorWhenUpdateFullNameAndLanguageThenUpdatesAndReturnsUpdatedProfile() {
        Customer customer = new Customer("Old Name", "+998901234567", LanguageCode.UZ, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(customer, "id", 42L);

        when(customerRepository.findById(42L)).thenReturn(Optional.of(customer));

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        MobileProfilePatchRequest request = new MobileProfilePatchRequest("New Name", "ru");

        MobileProfileResponse response = service.updateProfile(actor, request);

        assertThat(response.fullName()).isEqualTo("New Name");
        assertThat(response.preferredLanguage()).isEqualTo("ru");
        assertThat(customer.getFullName()).isEqualTo("New Name");
        assertThat(customer.getPreferredLanguage()).isEqualTo(LanguageCode.RU);
    }

    @Test
    void givenCustomerActorWhenUpdatePartialLanguageOnlyThenPreservesFullName() {
        Customer customer = new Customer("Preserved Name", "+998901234567", LanguageCode.UZ, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(customer, "id", 42L);

        when(customerRepository.findById(42L)).thenReturn(Optional.of(customer));

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        MobileProfilePatchRequest request = new MobileProfilePatchRequest(null, "en");

        MobileProfileResponse response = service.updateProfile(actor, request);

        assertThat(response.fullName()).isEqualTo("Preserved Name");
        assertThat(response.preferredLanguage()).isEqualTo("en");
        assertThat(customer.getFullName()).isEqualTo("Preserved Name");
        assertThat(customer.getPreferredLanguage()).isEqualTo(LanguageCode.EN);
    }

    @Test
    void givenCustomerActorWhenUpdateBlankFullNameThenThrowsValidationError() {
        Customer customer = new Customer("Old Name", "+998901234567", LanguageCode.UZ, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(customer, "id", 42L);

        when(customerRepository.findById(42L)).thenReturn(Optional.of(customer));

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        MobileProfilePatchRequest request = new MobileProfilePatchRequest("   ", null);

        BusinessException exception = catchThrowableOfType(
                () -> service.updateProfile(actor, request),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void givenCustomerActorWhenUpdateInvalidLanguageThenThrowsValidationError() {
        Customer customer = new Customer("Old Name", "+998901234567", LanguageCode.UZ, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(customer, "id", 42L);

        when(customerRepository.findById(42L)).thenReturn(Optional.of(customer));

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        MobileProfilePatchRequest request = new MobileProfilePatchRequest(null, "invalid-lang");

        BusinessException exception = catchThrowableOfType(
                () -> service.updateProfile(actor, request),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void givenTechnicianActorWhenUpdateLanguageThenUpdatesAndReturnsUpdatedProfile() {
        Technician technician = new Technician("Aziz Karimov", "+998901112233", "Washer", "Notes", 5, LanguageCode.UZ, true, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(technician, "id", 17L);

        when(technicianRepository.findById(17L)).thenReturn(Optional.of(technician));

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 17L, "+998901112233", true);
        MobileProfilePatchRequest request = new MobileProfilePatchRequest(null, "ru");

        MobileProfileResponse response = service.updateProfile(actor, request);

        assertThat(response.preferredLanguage()).isEqualTo("ru");
        assertThat(technician.getPreferredLanguage()).isEqualTo(LanguageCode.RU);
    }

    @Test
    void givenTechnicianActorWhenSuppliesFullNameThenFullNameIsIgnoredAndPreserved() {
        Technician technician = new Technician("Aziz Karimov", "+998901112233", "Washer", "Notes", 5, LanguageCode.UZ, true, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(technician, "id", 17L);

        when(technicianRepository.findById(17L)).thenReturn(Optional.of(technician));

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 17L, "+998901112233", true);
        MobileProfilePatchRequest request = new MobileProfilePatchRequest("Attempted Name Change", "en");

        MobileProfileResponse response = service.updateProfile(actor, request);

        assertThat(response.fullName()).isEqualTo("Aziz Karimov");
        assertThat(response.preferredLanguage()).isEqualTo("en");
        assertThat(technician.getFullName()).isEqualTo("Aziz Karimov");
        assertThat(technician.getPreferredLanguage()).isEqualTo(LanguageCode.EN);
    }
}
