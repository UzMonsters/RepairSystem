package com.example.darks.repair_auto.repair.action.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.repair.action.domain.RepairAvailableAction;
import com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentStatus;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository;
import com.example.darks.repair_auto.repair.execution.domain.RepairExecution;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.review.infrastructure.RepairReviewRepository;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RepairActionCapabilityServiceTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-18T10:00:00Z");

    private RepairAttachmentRepository attachmentRepository;
    private RepairReviewRepository reviewRepository;
    private RepairActionCapabilityService capabilityService;

    private Customer customer;
    private RepairCategory category;
    private User admin;
    private Technician technician;

    @BeforeEach
    void setUp() {
        attachmentRepository = mock(RepairAttachmentRepository.class);
        reviewRepository = mock(RepairReviewRepository.class);
        capabilityService = new RepairActionCapabilityService(attachmentRepository, reviewRepository);

        customer = new Customer("Ali Valiyev", "+998901234567", LanguageCode.UZ, NOW);
        ReflectionTestUtils.setField(customer, "id", 101L);

        category = mock(RepairCategory.class);
        when(category.getId()).thenReturn(4L);

        admin = new User("Admin", "admin@example.com", "hash", UserRole.ADMIN, true, NOW);
        ReflectionTestUtils.setField(admin, "id", 1L);

        technician = new Technician("Aziz Karimov", "+998901112233", "Cooling Master", "Notes", 5, LanguageCode.RU, true, NOW);
        ReflectionTestUtils.setField(technician, "id", 17L);
    }

    @Test
    void givenNullInputs_whenResolveTechnicianActions_thenReturnsEmptyList() {
        assertThat(capabilityService.resolveTechnicianActions(null, null, null)).isEmpty();
    }

    @Test
    void givenPendingAssignment_whenResolveTechnicianActions_thenReturnsAcceptAndReject() {
        RepairRequest request = new RepairRequest("REQ-2026-000042", customer, category, "Problem", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        ReflectionTestUtils.setField(request, "id", 42L);

        RepairAssignment assignment = new RepairAssignment(request, technician, NOW.plusDays(1), admin, NOW);
        ReflectionTestUtils.setField(assignment, "id", 19L);

        List<RepairAvailableAction> actions = capabilityService.resolveTechnicianActions(request, assignment, null);

        assertThat(actions).containsExactly(
                RepairAvailableAction.ACCEPT_ASSIGNMENT,
                RepairAvailableAction.REJECT_ASSIGNMENT);
    }

    @Test
    void givenAcceptedAssignedJob_whenResolveTechnicianActions_thenReturnsStartRepair() {
        RepairRequest request = new RepairRequest("REQ-2026-000042", customer, category, "Problem", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        request.markAssigned(NOW);
        ReflectionTestUtils.setField(request, "id", 42L);

        RepairAssignment assignment = new RepairAssignment(request, technician, NOW.plusDays(1), admin, NOW);
        assignment.accept(NOW);

        List<RepairAvailableAction> actions = capabilityService.resolveTechnicianActions(request, assignment, null);

        assertThat(actions).containsExactly(RepairAvailableAction.START_REPAIR);
    }

    @Test
    void givenAcceptedScheduledJob_whenResolveTechnicianActions_thenReturnsStartRepair() {
        RepairRequest request = new RepairRequest("REQ-2026-000042", customer, category, "Problem", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        request.markScheduled(NOW);
        ReflectionTestUtils.setField(request, "id", 42L);

        RepairAssignment assignment = new RepairAssignment(request, technician, NOW.plusDays(1), admin, NOW);
        assignment.accept(NOW);

        List<RepairAvailableAction> actions = capabilityService.resolveTechnicianActions(request, assignment, null);

        assertThat(actions).containsExactly(RepairAvailableAction.START_REPAIR);
    }

    @Test
    void givenInProgressJob_withoutDiagnosis_thenReturnsUpdateDiagnosisAndWaitForPartsAndUploadPhotos() {
        RepairRequest request = new RepairRequest("REQ-2026-000042", customer, category, "Problem", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        request.markInProgress(NOW);
        ReflectionTestUtils.setField(request, "id", 42L);

        RepairAssignment assignment = new RepairAssignment(request, technician, NOW.plusDays(1), admin, NOW);
        assignment.accept(NOW);

        RepairExecution execution = new RepairExecution(request, NOW);
        execution.startByTechnician(technician, NOW);

        List<RepairAvailableAction> actions = capabilityService.resolveTechnicianActions(request, assignment, execution);

        assertThat(actions).containsExactly(
                RepairAvailableAction.UPDATE_DIAGNOSIS,
                RepairAvailableAction.WAIT_FOR_PARTS,
                RepairAvailableAction.UPLOAD_DIAGNOSIS_PHOTO,
                RepairAvailableAction.UPLOAD_COMPLETION_PHOTO);
    }

    @Test
    void givenInProgressJob_withDiagnosis_withoutCompletionPhoto_thenReturnsUpdateDiagnosisAndWaitForPartsAndUploadPhotos() {
        RepairRequest request = new RepairRequest("REQ-2026-000042", customer, category, "Problem", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        request.markInProgress(NOW);
        ReflectionTestUtils.setField(request, "id", 42L);

        RepairAssignment assignment = new RepairAssignment(request, technician, NOW.plusDays(1), admin, NOW);
        assignment.accept(NOW);

        RepairExecution execution = new RepairExecution(request, NOW);
        execution.startByTechnician(technician, NOW);
        execution.updateDiagnosisByTechnician("Compressor failure", technician, NOW);

        when(attachmentRepository.countByRepairRequestIdAndAttachmentTypeAndStatus(
                eq(42L), eq(AttachmentType.COMPLETION_PHOTO), eq(AttachmentStatus.AVAILABLE)))
                .thenReturn(0L);

        List<RepairAvailableAction> actions = capabilityService.resolveTechnicianActions(request, assignment, execution);

        assertThat(actions).containsExactly(
                RepairAvailableAction.UPDATE_DIAGNOSIS,
                RepairAvailableAction.WAIT_FOR_PARTS,
                RepairAvailableAction.UPLOAD_DIAGNOSIS_PHOTO,
                RepairAvailableAction.UPLOAD_COMPLETION_PHOTO);
    }

    @Test
    void givenInProgressJob_withDiagnosis_withAvailableCompletionPhoto_thenIncludesCompleteRepair() {
        RepairRequest request = new RepairRequest("REQ-2026-000042", customer, category, "Problem", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        request.markInProgress(NOW);
        ReflectionTestUtils.setField(request, "id", 42L);

        RepairAssignment assignment = new RepairAssignment(request, technician, NOW.plusDays(1), admin, NOW);
        assignment.accept(NOW);

        RepairExecution execution = new RepairExecution(request, NOW);
        execution.startByTechnician(technician, NOW);
        execution.updateDiagnosisByTechnician("Compressor failure", technician, NOW);

        when(attachmentRepository.countByRepairRequestIdAndAttachmentTypeAndStatus(
                eq(42L), eq(AttachmentType.COMPLETION_PHOTO), eq(AttachmentStatus.AVAILABLE)))
                .thenReturn(1L);

        List<RepairAvailableAction> actions = capabilityService.resolveTechnicianActions(request, assignment, execution);

        assertThat(actions).containsExactly(
                RepairAvailableAction.UPDATE_DIAGNOSIS,
                RepairAvailableAction.WAIT_FOR_PARTS,
                RepairAvailableAction.UPLOAD_DIAGNOSIS_PHOTO,
                RepairAvailableAction.UPLOAD_COMPLETION_PHOTO,
                RepairAvailableAction.COMPLETE_REPAIR);
    }

    @Test
    void givenWaitingForPartsJob_whenResolveTechnicianActions_thenReturnsUpdateDiagnosisResumeRepairAndUploadDiagnosisPhoto() {
        RepairRequest request = new RepairRequest("REQ-2026-000042", customer, category, "Problem", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        request.markWaitingForParts(NOW);
        ReflectionTestUtils.setField(request, "id", 42L);

        RepairAssignment assignment = new RepairAssignment(request, technician, NOW.plusDays(1), admin, NOW);
        assignment.accept(NOW);

        RepairExecution execution = new RepairExecution(request, NOW);
        execution.startByTechnician(technician, NOW);
        execution.waitForParts("Need capacitor", NOW);

        List<RepairAvailableAction> actions = capabilityService.resolveTechnicianActions(request, assignment, execution);

        assertThat(actions).containsExactly(
                RepairAvailableAction.UPDATE_DIAGNOSIS,
                RepairAvailableAction.RESUME_REPAIR,
                RepairAvailableAction.UPLOAD_DIAGNOSIS_PHOTO);
    }

    @Test
    void givenCompletedJob_whenResolveTechnicianActions_thenReturnsEmptyList() {
        RepairRequest request = new RepairRequest("REQ-2026-000042", customer, category, "Problem", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        request.markCompleted(NOW);
        ReflectionTestUtils.setField(request, "id", 42L);

        RepairAssignment assignment = new RepairAssignment(request, technician, NOW.plusDays(1), admin, NOW);
        assignment.complete(NOW);

        List<RepairAvailableAction> actions = capabilityService.resolveTechnicianActions(request, assignment, null);

        assertThat(actions).isEmpty();
    }

    @Test
    void givenCancelledJob_whenResolveTechnicianActions_thenReturnsEmptyList() {
        RepairRequest request = new RepairRequest("REQ-2026-000042", customer, category, "Problem", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        request.markCancelled(NOW);
        ReflectionTestUtils.setField(request, "id", 42L);

        RepairAssignment assignment = new RepairAssignment(request, technician, NOW.plusDays(1), admin, NOW);
        assignment.cancel("Cancelled", NOW);

        List<RepairAvailableAction> actions = capabilityService.resolveTechnicianActions(request, assignment, null);

        assertThat(actions).isEmpty();
    }

    @Test
    void givenActiveCustomerRequest_whenResolveCustomerActions_thenReturnsUploadProblemPhoto() {
        RepairRequest request = new RepairRequest("REQ-2026-000042", customer, category, "Problem", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        ReflectionTestUtils.setField(request, "id", 42L);

        List<RepairAvailableAction> actions = capabilityService.resolveCustomerActions(request);

        assertThat(actions).containsExactly(RepairAvailableAction.UPLOAD_PROBLEM_PHOTO);
    }

    @Test
    void givenCompletedCustomerRequest_withoutReview_whenResolveCustomerActions_thenReturnsSubmitReview() {
        RepairRequest request = new RepairRequest("REQ-2026-000042", customer, category, "Problem", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        request.markCompleted(NOW);
        ReflectionTestUtils.setField(request, "id", 42L);

        when(reviewRepository.existsByRepairRequestId(42L)).thenReturn(false);

        List<RepairAvailableAction> actions = capabilityService.resolveCustomerActions(request);

        assertThat(actions).containsExactly(RepairAvailableAction.SUBMIT_REVIEW);
    }

    @Test
    void givenCompletedCustomerRequest_withReview_whenResolveCustomerActions_thenReturnsEmptyList() {
        RepairRequest request = new RepairRequest("REQ-2026-000042", customer, category, "Problem", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        request.markCompleted(NOW);
        ReflectionTestUtils.setField(request, "id", 42L);

        when(reviewRepository.existsByRepairRequestId(42L)).thenReturn(true);

        List<RepairAvailableAction> actions = capabilityService.resolveCustomerActions(request);

        assertThat(actions).isEmpty();
    }

    @Test
    void givenCancelledCustomerRequest_whenResolveCustomerActions_thenReturnsEmptyList() {
        RepairRequest cancelled = new RepairRequest("REQ-2026-000043", customer, category, "Problem", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        cancelled.markCancelled(NOW);

        assertThat(capabilityService.resolveCustomerActions(cancelled)).isEmpty();
    }
}
