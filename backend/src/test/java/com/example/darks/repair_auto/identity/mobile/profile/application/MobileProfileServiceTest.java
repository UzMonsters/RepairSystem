package com.example.darks.repair_auto.identity.mobile.profile.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.mobile.profile.api.dto.MobileProfilePatchRequest;
import com.example.darks.repair_auto.identity.mobile.profile.api.dto.MobileProfileResponse;
import com.example.darks.repair_auto.profile.api.dto.AvatarResponse;
import com.example.darks.repair_auto.repair.attachment.application.AttachmentDownload;
import com.example.darks.repair_auto.repair.attachment.application.AttachmentValidator;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentStatus;
import com.example.darks.repair_auto.repair.attachment.domain.RepairAttachment;
import com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.ObjectStorageService;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StoredObjectDownload;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class MobileProfileServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-18T10:00:00Z");

    private CustomerRepository customerRepository;
    private TechnicianRepository technicianRepository;
    private RepairAttachmentRepository attachmentRepository;
    private ObjectStorageService objectStorageService;
    private AttachmentValidator validator;
    private MobileProfileService service;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        technicianRepository = mock(TechnicianRepository.class);
        attachmentRepository = mock(RepairAttachmentRepository.class);
        objectStorageService = mock(ObjectStorageService.class);
        validator = new AttachmentValidator();
        service = new MobileProfileService(
                customerRepository,
                technicianRepository,
                attachmentRepository,
                objectStorageService,
                validator,
                Clock.fixed(NOW, ZoneOffset.UTC));
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

    @Test
    void givenCustomerActor_whenUploadAvatar_thenUploadsAndReturnsAvatarResponse() {
        Customer customer = new Customer("Ali Valiyev", "+998901234567", LanguageCode.UZ, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(customer, "id", 42L);

        when(customerRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(customer));
        when(attachmentRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            RepairAttachment att = invocation.getArgument(0);
            ReflectionTestUtils.setField(att, "id", 701L);
            return att;
        });

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        // Valid JPEG header bytes
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1, 1, 0, 0, 1};
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", jpegBytes);

        AvatarResponse response = service.uploadAvatar(actor, file);

        assertThat(response).isNotNull();
        assertThat(response.attachmentId()).isEqualTo(701L);
        assertThat(response.fileName()).isEqualTo("avatar.jpg");
        assertThat(response.contentType()).isEqualTo("image/jpeg");
        assertThat(response.downloadUrl()).isEqualTo("/api/v1/mobile/me/avatar");
        assertThat(customer.getAvatarAttachment()).isNotNull();
    }

    @Test
    void givenCustomerActor_whenDownloadAvatar_thenReturnsStream() {
        Customer customer = new Customer("Ali Valiyev", "+998901234567", LanguageCode.UZ, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(customer, "id", 42L);

        RepairAttachment avatar = RepairAttachment.customerUpload(
                null,
                com.example.darks.repair_auto.repair.attachment.domain.AttachmentType.AVATAR,
                "avatars/customers/42/key.jpg",
                "avatar.jpg",
                customer,
                OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)
        );
        avatar.markAvailable("image/jpeg", 16L, "checksum", OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(avatar, "id", 701L);
        customer.setAvatarAttachment(avatar, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));

        when(customerRepository.findById(42L)).thenReturn(Optional.of(customer));
        when(objectStorageService.download("avatars/customers/42/key.jpg"))
                .thenReturn(new StoredObjectDownload("image/jpeg", 3L, new ByteArrayInputStream(new byte[]{1, 2, 3})));

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        AttachmentDownload download = service.downloadAvatar(actor);

        assertThat(download).isNotNull();
        assertThat(download.fileName()).isEqualTo("avatar.jpg");
        assertThat(download.contentType()).isEqualTo("image/jpeg");
        assertThat(download.sizeBytes()).isEqualTo(3L);
    }

    @Test
    void givenCustomerActor_whenDeleteAvatar_thenClearsAvatarAndMarksDeleted() {
        Customer customer = new Customer("Ali Valiyev", "+998901234567", LanguageCode.UZ, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(customer, "id", 42L);

        RepairAttachment avatar = RepairAttachment.customerUpload(
                null,
                com.example.darks.repair_auto.repair.attachment.domain.AttachmentType.AVATAR,
                "avatars/customers/42/key.jpg",
                "avatar.jpg",
                customer,
                OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)
        );
        avatar.markAvailable("image/jpeg", 16L, "checksum", OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(avatar, "id", 701L);
        customer.setAvatarAttachment(avatar, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));

        when(customerRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(customer));

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        service.deleteAvatar(actor);

        assertThat(customer.getAvatarAttachment()).isNull();
        assertThat(avatar.getStatus()).isEqualTo(AttachmentStatus.DELETED);
    }

    @Test
    void givenTechnicianActor_whenUploadAvatar_thenUploadsAndReturnsAvatarResponse() {
        Technician technician = new Technician("Aziz Karimov", "+998901112233", "Washer", "Notes", 5, LanguageCode.UZ, true, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(technician, "id", 17L);

        when(technicianRepository.findByIdForUpdate(17L)).thenReturn(Optional.of(technician));
        when(attachmentRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            RepairAttachment att = invocation.getArgument(0);
            ReflectionTestUtils.setField(att, "id", 702L);
            return att;
        });

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 17L, "+998901112233", true);
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1, 1, 0, 0, 1};
        MockMultipartFile file = new MockMultipartFile("file", "tech.jpg", "image/jpeg", jpegBytes);

        AvatarResponse response = service.uploadAvatar(actor, file);

        assertThat(response).isNotNull();
        assertThat(response.attachmentId()).isEqualTo(702L);
        assertThat(response.downloadUrl()).isEqualTo("/api/v1/mobile/me/avatar");
        assertThat(technician.getAvatarAttachment()).isNotNull();
    }
}
