package com.example.darks.repair_auto.repair.access.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentStatus;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.repair.attachment.domain.RepairAttachment;
import com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RepairResourceAccessPolicyTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-18T10:00:00Z");

    private RepairRequestRepository repairRequestRepository;
    private RepairAssignmentRepository repairAssignmentRepository;
    private RepairAttachmentRepository repairAttachmentRepository;
    private RepairResourceAccessPolicy policy;

    private Customer customer;
    private RepairCategory category;
    private User admin;
    private Technician technician;

    @BeforeEach
    void setUp() {
        repairRequestRepository = mock(RepairRequestRepository.class);
        repairAssignmentRepository = mock(RepairAssignmentRepository.class);
        repairAttachmentRepository = mock(RepairAttachmentRepository.class);

        policy = new RepairResourceAccessPolicy(
                repairRequestRepository,
                repairAssignmentRepository,
                repairAttachmentRepository);

        customer = new Customer("Customer A", "+998901111111", LanguageCode.UZ, NOW);
        ReflectionTestUtils.setField(customer, "id", 101L);

        category = mock(RepairCategory.class);
        when(category.getId()).thenReturn(1L);

        admin = new User("Admin", "admin@example.com", "hash", UserRole.ADMIN, true, NOW);
        ReflectionTestUtils.setField(admin, "id", 1L);

        technician = new Technician("Tech A", "+998902222222", "Master", "Notes", 5, LanguageCode.RU, true, NOW);
        ReflectionTestUtils.setField(technician, "id", 202L);
    }

    @Test
    void givenCustomerOwnsRequest_whenRequireCustomerOwnsRequest_thenReturnsRequest() {
        RepairRequest request = new RepairRequest("REQ-101", customer, category, "Fix motor", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        ReflectionTestUtils.setField(request, "id", 501L);

        when(repairRequestRepository.findByIdAndCustomerId(501L, 101L)).thenReturn(Optional.of(request));

        RepairRequest result = policy.requireCustomerOwnsRequest(101L, 501L);

        assertThat(result).isSameAs(request);
    }

    @Test
    void givenCustomerDoesNotOwnRequest_whenRequireCustomerOwnsRequest_thenThrowsNotFound() {
        when(repairRequestRepository.findByIdAndCustomerId(501L, 999L)).thenReturn(Optional.empty());

        BusinessException exception = catchThrowableOfType(
                () -> policy.requireCustomerOwnsRequest(999L, 501L),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REPAIR_REQUEST_NOT_FOUND);
    }

    @Test
    void givenCompletedOrCancelledRequest_whenRequireCustomerCanReadRequest_thenReturnsRequest() {
        RepairRequest request = new RepairRequest("REQ-101", customer, category, "Fix motor", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        request.markCompleted(NOW);
        ReflectionTestUtils.setField(request, "id", 501L);

        when(repairRequestRepository.findByIdAndCustomerId(501L, 101L)).thenReturn(Optional.of(request));

        RepairRequest result = policy.requireCustomerCanReadRequest(101L, 501L);

        assertThat(result.getStatus().name()).isEqualTo("COMPLETED");
    }

    @Test
    void givenNullParameters_whenRequireCustomerOwnsRequest_thenThrowsValidationError() {
        BusinessException exception = catchThrowableOfType(
                () -> policy.requireCustomerOwnsRequest(null, 501L),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void givenCustomerOwnsAttachmentRequest_whenRequireCustomerCanAccessAttachment_thenReturnsAttachment() {
        RepairRequest request = new RepairRequest("REQ-101", customer, category, "Fix motor", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        ReflectionTestUtils.setField(request, "id", 501L);

        RepairAttachment attachment = RepairAttachment.customerUpload(request, AttachmentType.CUSTOMER_PROBLEM_PHOTO, "keys/1", "photo.jpg", customer, NOW);
        attachment.markAvailable("image/jpeg", 1024L, "checksum", NOW);
        ReflectionTestUtils.setField(attachment, "id", 701L);

        when(repairAttachmentRepository.findByIdAndStatus(701L, AttachmentStatus.AVAILABLE)).thenReturn(Optional.of(attachment));
        when(repairRequestRepository.findByIdAndCustomerId(501L, 101L)).thenReturn(Optional.of(request));

        RepairAttachment result = policy.requireCustomerCanAccessAttachment(101L, 701L);

        assertThat(result).isSameAs(attachment);
    }

    @Test
    void givenCustomerDoesNotOwnAttachmentRequest_whenRequireCustomerCanAccessAttachment_thenThrowsNotFound() {
        RepairRequest request = new RepairRequest("REQ-101", customer, category, "Fix motor", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        ReflectionTestUtils.setField(request, "id", 501L);

        RepairAttachment attachment = RepairAttachment.customerUpload(request, AttachmentType.CUSTOMER_PROBLEM_PHOTO, "keys/1", "photo.jpg", customer, NOW);
        attachment.markAvailable("image/jpeg", 1024L, "checksum", NOW);
        ReflectionTestUtils.setField(attachment, "id", 701L);

        when(repairAttachmentRepository.findByIdAndStatus(701L, AttachmentStatus.AVAILABLE)).thenReturn(Optional.of(attachment));
        when(repairRequestRepository.findByIdAndCustomerId(501L, 999L)).thenReturn(Optional.empty());

        BusinessException exception = catchThrowableOfType(
                () -> policy.requireCustomerCanAccessAttachment(999L, 701L),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REPAIR_REQUEST_NOT_FOUND);
    }

    @Test
    void givenTechnicianActiveAssignment_whenRequireTechnicianCurrentAssignment_thenReturnsAssignment() {
        RepairRequest request = new RepairRequest("REQ-101", customer, category, "Fix motor", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        ReflectionTestUtils.setField(request, "id", 501L);

        RepairAssignment assignment = new RepairAssignment(request, technician, NOW.plusDays(1), admin, NOW);
        ReflectionTestUtils.setField(assignment, "id", 301L);

        when(repairAssignmentRepository.findActiveByRequestId(501L, RepairResourceAccessPolicy.ACTIVE_ASSIGNMENT_STATUSES))
                .thenReturn(Optional.of(assignment));

        RepairAssignment result = policy.requireTechnicianCurrentAssignment(202L, 501L);

        assertThat(result).isSameAs(assignment);
    }

    @Test
    void givenOtherTechnicianActiveAssignment_whenRequireTechnicianCurrentAssignment_thenThrowsNotFound() {
        RepairRequest request = new RepairRequest("REQ-101", customer, category, "Fix motor", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        ReflectionTestUtils.setField(request, "id", 501L);

        RepairAssignment assignment = new RepairAssignment(request, technician, NOW.plusDays(1), admin, NOW);
        ReflectionTestUtils.setField(assignment, "id", 301L);

        when(repairAssignmentRepository.findActiveByRequestId(501L, RepairResourceAccessPolicy.ACTIVE_ASSIGNMENT_STATUSES))
                .thenReturn(Optional.of(assignment));

        BusinessException exception = catchThrowableOfType(
                () -> policy.requireTechnicianCurrentAssignment(999L, 501L),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVE_ASSIGNMENT_NOT_FOUND);
    }

    @Test
    void givenTechnicianAssignedPendingOrAcceptedOrCompletedOrCancelled_whenRequireTechnicianCanReadRequest_thenReturnsRequest() {
        RepairRequest request = new RepairRequest("REQ-101", customer, category, "Fix motor", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        ReflectionTestUtils.setField(request, "id", 501L);

        RepairAssignment assignment = new RepairAssignment(request, technician, NOW.plusDays(1), admin, NOW);
        assignment.complete(NOW);

        when(repairRequestRepository.findWithRelationsById(501L)).thenReturn(Optional.of(request));
        when(repairAssignmentRepository.findByRepairRequestIdAndTechnicianIdAndStatusInOrderByCreatedAtDesc(
                eq(501L), eq(202L), any())).thenReturn(List.of(assignment));

        RepairRequest result = policy.requireTechnicianCanReadRequest(202L, 501L);

        assertThat(result).isSameAs(request);
    }

    @Test
    void givenTechnicianRejectedAssignmentOnly_whenRequireTechnicianCanReadRequest_thenThrowsNotFound() {
        RepairRequest request = new RepairRequest("REQ-101", customer, category, "Fix motor", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        ReflectionTestUtils.setField(request, "id", 501L);

        when(repairRequestRepository.findWithRelationsById(501L)).thenReturn(Optional.of(request));
        when(repairAssignmentRepository.findByRepairRequestIdAndTechnicianIdAndStatusInOrderByCreatedAtDesc(
                eq(501L), eq(202L), any())).thenReturn(Collections.emptyList());

        BusinessException exception = catchThrowableOfType(
                () -> policy.requireTechnicianCanReadRequest(202L, 501L),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REPAIR_REQUEST_NOT_FOUND);
    }

    @Test
    void givenCustomerActor_whenCallingCurrentCustomerHelpers_thenSucceeds() {
        RepairRequest request = new RepairRequest("REQ-101", customer, category, "Fix motor", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        ReflectionTestUtils.setField(request, "id", 501L);

        when(repairRequestRepository.findByIdAndCustomerId(501L, 101L)).thenReturn(Optional.of(request));

        AuthenticatedMobileActor customerActor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 101L, "+998901111111", true);

        RepairRequest result = policy.requireCurrentCustomerOwnsRequest(customerActor, 501L);

        assertThat(result).isSameAs(request);
    }

    @Test
    void givenCustomerActor_whenCallingTechnicianMethod_thenThrowsAccessDenied() {
        AuthenticatedMobileActor customerActor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 101L, "+998901111111", true);

        BusinessException exception = catchThrowableOfType(
                () -> policy.requireCurrentTechnicianCanReadRequest(customerActor, 501L),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    void givenTechnicianActor_whenCallingCurrentTechnicianHelper_thenSucceeds() {
        RepairRequest request = new RepairRequest(
                "REQ-202",
                customer,
                category,
                "Fix pump",
                "Tashkent",
                null,
                null,
                RepairRequestPriority.NORMAL,
                null,
                null,
                admin,
                NOW);
        ReflectionTestUtils.setField(request, "id", 501L);

        when(repairRequestRepository.findWithRelationsById(501L)).thenReturn(Optional.of(request));
        when(repairAssignmentRepository.findByRepairRequestIdAndTechnicianIdAndStatusInOrderByCreatedAtDesc(
                eq(501L), eq(202L), any())).thenReturn(List.of(mock(RepairAssignment.class)));

        AuthenticatedMobileActor techActor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 202L, "+998902222222", true);

        RepairRequest result = policy.requireCurrentTechnicianCanReadRequest(techActor, 501L);

        assertThat(result).isSameAs(request);
    }

    @Test
    void givenTechnicianActor_whenCallingCustomerMethod_thenThrowsAccessDenied() {
        AuthenticatedMobileActor techActor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 202L, "+998902222222", true);

        BusinessException exception = catchThrowableOfType(
                () -> policy.requireCurrentCustomerOwnsRequest(techActor, 501L),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    void givenInactiveActor_whenCallingCurrentActorMethods_thenThrowsAccountInactive() {
        AuthenticatedMobileActor inactiveActor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 101L, "+998901111111", false);

        BusinessException exception = catchThrowableOfType(
                () -> policy.requireCurrentCustomerOwnsRequest(inactiveActor, 501L),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_INACTIVE);
    }
}
