package com.example.darks.repair_auto.repair.attachment.mobile.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.repair.access.application.RepairResourceAccessPolicy;
import com.example.darks.repair_auto.repair.attachment.api.dto.AttachmentResponse;
import com.example.darks.repair_auto.repair.attachment.api.dto.DownloadUrlResponse;
import com.example.darks.repair_auto.repair.attachment.application.AttachmentService;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentStatus;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.repair.attachment.domain.RepairAttachment;
import com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository;
import com.example.darks.repair_auto.repair.attachment.mobile.api.dto.MobileAttachmentDownloadUrlResponse;
import com.example.darks.repair_auto.repair.attachment.mobile.api.dto.MobileAttachmentResponse;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class MobileAttachmentFacadeTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-18T10:00:00Z");

    private AttachmentService attachmentService;
    private RepairResourceAccessPolicy accessPolicy;
    private RepairAttachmentRepository attachmentRepository;
    private MobileAttachmentFacade facade;

    private Customer customer;
    private Technician technician;
    private User admin;
    private RepairCategory category;
    private RepairRequest request;

    @BeforeEach
    void setUp() {
        attachmentService = mock(AttachmentService.class);
        accessPolicy = mock(RepairResourceAccessPolicy.class);
        attachmentRepository = mock(RepairAttachmentRepository.class);

        facade = new MobileAttachmentFacade(
                attachmentService,
                accessPolicy,
                attachmentRepository);

        customer = new Customer("Ali Valiyev", "+998901234567", LanguageCode.UZ, NOW);
        ReflectionTestUtils.setField(customer, "id", 101L);

        technician = new Technician("Aziz Karimov", "+998901112233", "Cooling Master", "Notes", 5, LanguageCode.RU, true, NOW);
        ReflectionTestUtils.setField(technician, "id", 17L);

        admin = new User("Admin", "admin@example.com", "hash", UserRole.ADMIN, true, NOW);
        ReflectionTestUtils.setField(admin, "id", 1L);

        category = mock(RepairCategory.class);
        when(category.getId()).thenReturn(4L);

        request = new RepairRequest("REQ-2026-000042", customer, category, "Problem", "Chilanzar", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        ReflectionTestUtils.setField(request, "id", 42L);
    }

    @Test
    void givenCustomerActor_whenUploadProblemPhoto_thenDelegatesToAttachmentService() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 101L, "+998901234567", true);
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        AttachmentResponse serviceResponse = new AttachmentResponse(
                501L,
                42L,
                AttachmentType.CUSTOMER_PROBLEM_PHOTO,
                "photo.jpg",
                "image/jpeg",
                3L,
                AttachmentStatus.AVAILABLE,
                null,
                null,
                NOW);

        when(attachmentService.uploadFromCustomer(
                eq(42L),
                eq(AttachmentType.CUSTOMER_PROBLEM_PHOTO),
                eq("photo.jpg"),
                eq("image/jpeg"),
                eq(3L),
                any(InputStream.class),
                eq(101L))).thenReturn(serviceResponse);

        MobileAttachmentResponse response = facade.uploadCustomerAttachment(
                actor,
                42L,
                AttachmentType.CUSTOMER_PROBLEM_PHOTO,
                file);

        verify(accessPolicy).requireCurrentCustomerOwnsRequest(actor, 42L);
        assertThat(response.id()).isEqualTo(501L);
        assertThat(response.type()).isEqualTo(AttachmentType.CUSTOMER_PROBLEM_PHOTO);
        assertThat(response.originalFileName()).isEqualTo("photo.jpg");
        assertThat(response.status()).isEqualTo(AttachmentStatus.AVAILABLE);
    }

    @Test
    void givenCustomerActor_whenUploadInvalidType_thenThrowsBusinessRuleException() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 101L, "+998901234567", true);
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> facade.uploadCustomerAttachment(actor, 42L, AttachmentType.COMPLETION_PHOTO, file))
                .isInstanceOf(BusinessRuleException.class)
                .matches(e -> ((BusinessRuleException) e).getErrorCode() == ErrorCode.ATTACHMENT_TYPE_NOT_ALLOWED);
    }

    @Test
    void givenCustomerActor_whenListAttachments_thenReturnsRoleVisibleAttachments() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 101L, "+998901234567", true);

        RepairAttachment attachment1 = RepairAttachment.customerUpload(
                request, AttachmentType.CUSTOMER_PROBLEM_PHOTO, "key1", "problem.jpg", customer, NOW);
        ReflectionTestUtils.setField(attachment1, "id", 501L);
        ReflectionTestUtils.setField(attachment1, "status", AttachmentStatus.AVAILABLE);
        ReflectionTestUtils.setField(attachment1, "contentType", "image/jpeg");
        ReflectionTestUtils.setField(attachment1, "sizeBytes", 1024L);

        when(attachmentRepository.findByRepairRequestIdAndStatusAndAttachmentTypeInOrderByUploadedAtAsc(
                eq(42L), eq(AttachmentStatus.AVAILABLE), any()))
                .thenReturn(List.of(attachment1));

        List<MobileAttachmentResponse> list = facade.listCustomerAttachments(actor, 42L);

        verify(accessPolicy).requireCurrentCustomerCanReadRequest(actor, 42L);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).id()).isEqualTo(501L);
        assertThat(list.get(0).type()).isEqualTo(AttachmentType.CUSTOMER_PROBLEM_PHOTO);
    }

    @Test
    void givenTechnicianActor_whenUploadDiagnosisPhoto_thenDelegatesToAttachmentService() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 17L, "+998901112233", true);
        MockMultipartFile file = new MockMultipartFile("file", "diagnosis.jpg", "image/jpeg", new byte[]{4, 5, 6});

        AttachmentResponse serviceResponse = new AttachmentResponse(
                502L,
                42L,
                AttachmentType.DIAGNOSIS_PHOTO,
                "diagnosis.jpg",
                "image/jpeg",
                3L,
                AttachmentStatus.AVAILABLE,
                null,
                null,
                NOW);

        when(attachmentService.uploadFromTechnician(
                eq(42L),
                eq(AttachmentType.DIAGNOSIS_PHOTO),
                eq("diagnosis.jpg"),
                eq("image/jpeg"),
                eq(3L),
                any(InputStream.class),
                eq(17L))).thenReturn(serviceResponse);

        MobileAttachmentResponse response = facade.uploadTechnicianAttachment(
                actor,
                42L,
                AttachmentType.DIAGNOSIS_PHOTO,
                file);

        verify(accessPolicy).requireCurrentTechnicianCurrentAssignment(actor, 42L);
        assertThat(response.id()).isEqualTo(502L);
        assertThat(response.type()).isEqualTo(AttachmentType.DIAGNOSIS_PHOTO);
    }

    @Test
    void givenTechnicianActor_whenUploadCompletionPhoto_thenDelegatesToAttachmentService() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 17L, "+998901112233", true);
        MockMultipartFile file = new MockMultipartFile("file", "complete.jpg", "image/jpeg", new byte[]{7, 8, 9});

        AttachmentResponse serviceResponse = new AttachmentResponse(
                503L,
                42L,
                AttachmentType.COMPLETION_PHOTO,
                "complete.jpg",
                "image/jpeg",
                3L,
                AttachmentStatus.AVAILABLE,
                null,
                null,
                NOW);

        when(attachmentService.uploadFromTechnician(
                eq(42L),
                eq(AttachmentType.COMPLETION_PHOTO),
                eq("complete.jpg"),
                eq("image/jpeg"),
                eq(3L),
                any(InputStream.class),
                eq(17L))).thenReturn(serviceResponse);

        MobileAttachmentResponse response = facade.uploadTechnicianAttachment(
                actor,
                42L,
                AttachmentType.COMPLETION_PHOTO,
                file);

        verify(accessPolicy).requireCurrentTechnicianCurrentAssignment(actor, 42L);
        assertThat(response.id()).isEqualTo(503L);
        assertThat(response.type()).isEqualTo(AttachmentType.COMPLETION_PHOTO);
    }

    @Test
    void givenTechnicianActor_whenUploadInvalidType_thenThrowsBusinessRuleException() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 17L, "+998901112233", true);
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> facade.uploadTechnicianAttachment(actor, 42L, AttachmentType.CUSTOMER_PROBLEM_PHOTO, file))
                .isInstanceOf(BusinessRuleException.class)
                .matches(e -> ((BusinessRuleException) e).getErrorCode() == ErrorCode.ATTACHMENT_TYPE_NOT_ALLOWED);
    }

    @Test
    void givenCustomerActor_whenGetDownloadUrlForVisibleAttachment_thenReturnsUrlResponse() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 101L, "+998901234567", true);
        RepairAttachment attachment = RepairAttachment.customerUpload(
                request, AttachmentType.CUSTOMER_PROBLEM_PHOTO, "key1", "problem.jpg", customer, NOW);
        ReflectionTestUtils.setField(attachment, "id", 501L);

        when(accessPolicy.requireCurrentCustomerCanAccessAttachment(actor, 501L)).thenReturn(attachment);
        when(attachmentService.downloadUrl(501L)).thenReturn(new DownloadUrlResponse("https://s3.example.com/download", NOW.plusMinutes(15)));

        MobileAttachmentDownloadUrlResponse response = facade.getDownloadUrl(actor, 501L);

        assertThat(response.attachmentId()).isEqualTo(501L);
        assertThat(response.url()).isEqualTo("https://s3.example.com/download");
        assertThat(response.expiresAt()).isEqualTo(NOW.plusMinutes(15));
    }

    @Test
    void givenCustomerActor_whenGetDownloadUrlForInternalAttachment_thenThrowsAttachmentNotFound() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 101L, "+998901234567", true);
        RepairAttachment attachment = new RepairAttachment(
                request, AttachmentType.GENERAL_DOCUMENT, "key1", "internal.pdf", admin, NOW);
        ReflectionTestUtils.setField(attachment, "id", 509L);

        when(accessPolicy.requireCurrentCustomerCanAccessAttachment(actor, 509L)).thenReturn(attachment);

        assertThatThrownBy(() -> facade.getDownloadUrl(actor, 509L))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.ATTACHMENT_NOT_FOUND);
    }
}
