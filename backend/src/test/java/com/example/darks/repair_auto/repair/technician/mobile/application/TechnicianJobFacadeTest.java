package com.example.darks.repair_auto.repair.technician.mobile.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
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
import com.example.darks.repair_auto.repair.action.application.RepairActionCapabilityService;
import com.example.darks.repair_auto.repair.action.domain.RepairAvailableAction;
import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentRejectionRequest;
import com.example.darks.repair_auto.repair.assignment.application.RepairAssignmentService;
import com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.execution.application.RepairExecutionService;
import com.example.darks.repair_auto.repair.execution.domain.RepairExecution;
import com.example.darks.repair_auto.repair.execution.infrastructure.RepairExecutionRepository;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.repair.technician.mobile.api.dto.TechnicianJobDetailResponse;
import com.example.darks.repair_auto.repair.technician.mobile.api.dto.TechnicianJobListView;
import com.example.darks.repair_auto.repair.technician.mobile.api.dto.TechnicianJobSummaryResponse;
import com.example.darks.repair_auto.repair.technician.mobile.api.dto.TechnicianScheduleItemResponse;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.shared.i18n.LocalizationService;
import com.example.darks.repair_auto.shared.i18n.RequestLocaleResolver;
import com.example.darks.repair_auto.shared.i18n.SupportedLanguage;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import com.example.darks.repair_auto.technician.domain.Technician;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class TechnicianJobFacadeTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-18T10:00:00Z");

    private RepairAssignmentService repairAssignmentService;
    private RepairExecutionService repairExecutionService;
    private RepairResourceAccessPolicy accessPolicy;
    private RepairAssignmentRepository repairAssignmentRepository;
    private RepairExecutionRepository repairExecutionRepository;
    private RepairActionCapabilityService actionCapabilityService;
    private RequestLocaleResolver requestLocaleResolver;
    private LocalizationService localizationService;
    private TechnicianJobFacade facade;

    private Customer customer;
    private RepairCategory category;
    private User admin;
    private Technician technician;

    @BeforeEach
    void setUp() {
        repairAssignmentService = mock(RepairAssignmentService.class);
        repairExecutionService = mock(RepairExecutionService.class);
        accessPolicy = mock(RepairResourceAccessPolicy.class);
        repairAssignmentRepository = mock(RepairAssignmentRepository.class);
        repairExecutionRepository = mock(RepairExecutionRepository.class);
        actionCapabilityService = mock(RepairActionCapabilityService.class);
        requestLocaleResolver = mock(RequestLocaleResolver.class);
        localizationService = mock(LocalizationService.class);

        facade = new TechnicianJobFacade(
                repairAssignmentService,
                repairExecutionService,
                accessPolicy,
                repairAssignmentRepository,
                repairExecutionRepository,
                actionCapabilityService,
                requestLocaleResolver,
                localizationService);

        customer = new Customer("Ali Valiyev", "+998901234567", LanguageCode.UZ, NOW);
        ReflectionTestUtils.setField(customer, "id", 101L);

        category = mock(RepairCategory.class);
        when(category.getId()).thenReturn(4L);
        when(category.getNameUz()).thenReturn("Konditsioner");
        when(category.getNameRu()).thenReturn("Кондиционер");
        when(category.getNameEn()).thenReturn("Air Conditioner");

        admin = new User("Admin", "admin@example.com", "hash", UserRole.ADMIN, true, NOW);
        ReflectionTestUtils.setField(admin, "id", 1L);

        technician = new Technician("Aziz Karimov", "+998901112233", "Cooling Master", "Notes", 5, LanguageCode.RU, true, NOW);
        ReflectionTestUtils.setField(technician, "id", 17L);

        when(requestLocaleResolver.resolveLanguage()).thenReturn(SupportedLanguage.UZ);
        when(localizationService.get(any(), any())).thenReturn("Localized Label");
    }

    @Test
    void givenTechnicianActor_whenListActiveJobs_thenReturnsPageOfSummaries() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 17L, "+998901112233", true);
        RepairRequest req = new RepairRequest("REQ-2026-000042", customer, category, "Problem", "Chilanzar", null, null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        ReflectionTestUtils.setField(req, "id", 42L);

        RepairAssignment assignment = new RepairAssignment(req, technician, NOW.plusDays(1), admin, NOW);
        ReflectionTestUtils.setField(assignment, "id", 19L);

        when(repairAssignmentRepository.findJobsByTechnicianIdAndStatusIn(eq(17L), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(assignment), PageRequest.of(0, 20), 1));

        PageResponse<TechnicianJobSummaryResponse> result = facade.listJobs(actor, TechnicianJobListView.ACTIVE, PageRequest.of(0, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).requestId()).isEqualTo(42L);
        assertThat(result.content().get(0).assignmentId()).isEqualTo(19L);
        assertThat(result.content().get(0).customer().fullName()).isEqualTo("Ali Valiyev");
        assertThat(result.content().get(0).category().name()).isEqualTo("Konditsioner");
    }

    @Test
    void givenTechnicianActor_whenGetJobDetail_thenReturnsTechnicianSafeDetail() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 17L, "+998901112233", true);
        RepairRequest req = new RepairRequest("REQ-2026-000042", customer, category, "Problem description", "Chilanzar 9", new BigDecimal("41.2"), new BigDecimal("69.2"), com.example.darks.repair_auto.repair.request.domain.RequestLocationSource.DEVICE_GPS, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        req.markInProgress(NOW);
        ReflectionTestUtils.setField(req, "id", 42L);

        RepairAssignment assignment = new RepairAssignment(req, technician, NOW.plusDays(1), admin, NOW);
        assignment.accept(NOW);
        ReflectionTestUtils.setField(assignment, "id", 19L);

        RepairExecution execution = new RepairExecution(req, NOW);
        ReflectionTestUtils.setField(execution, "diagnosis", "Capacitor failed");
        ReflectionTestUtils.setField(execution, "workPerformed", "Replaced capacitor");

        when(accessPolicy.requireCurrentTechnicianCanReadRequest(actor, 42L)).thenReturn(req);
        when(repairAssignmentRepository.findByRepairRequestIdAndTechnicianIdAndStatusInOrderByCreatedAtDesc(eq(42L), eq(17L), any()))
                .thenReturn(List.of(assignment));
        when(repairExecutionRepository.findByRepairRequestId(42L)).thenReturn(Optional.of(execution));
        when(actionCapabilityService.resolveTechnicianActions(eq(req), eq(assignment), eq(execution)))
                .thenReturn(List.of(RepairAvailableAction.UPDATE_DIAGNOSIS, RepairAvailableAction.WAIT_FOR_PARTS));

        TechnicianJobDetailResponse detail = facade.getJobDetail(actor, 42L);

        assertThat(detail.requestId()).isEqualTo(42L);
        assertThat(detail.assignmentId()).isEqualTo(19L);
        assertThat(detail.customer().phone()).isEqualTo("+998901234567");
        assertThat(detail.location().address()).isEqualTo("Chilanzar 9");
        assertThat(detail.execution().diagnosis()).isEqualTo("Capacitor failed");
        assertThat(detail.availableActions()).containsExactly(
                RepairAvailableAction.UPDATE_DIAGNOSIS,
                RepairAvailableAction.WAIT_FOR_PARTS);
    }

    @Test
    void givenTechnicianActor_whenGetSchedule_thenReturnsBoundedSchedule() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 17L, "+998901112233", true);
        RepairRequest req = new RepairRequest("REQ-2026-000042", customer, category, "Problem", "Chilanzar", null, null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        ReflectionTestUtils.setField(req, "id", 42L);

        RepairAssignment assignment = new RepairAssignment(req, technician, NOW.plusDays(1), admin, NOW);
        ReflectionTestUtils.setField(assignment, "id", 19L);

        when(repairAssignmentRepository.findSchedule(eq(17L), any(), any(), any()))
                .thenReturn(List.of(assignment));

        List<TechnicianScheduleItemResponse> schedule = facade.getSchedule(actor, LocalDate.of(2026, 8, 18), LocalDate.of(2026, 8, 25));

        assertThat(schedule).hasSize(1);
        assertThat(schedule.get(0).requestId()).isEqualTo(42L);
        assertThat(schedule.get(0).customer().fullName()).isEqualTo("Ali Valiyev");
    }

    @Test
    void givenInvalidScheduleRange_whenGetSchedule_thenThrowsValidationError() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 17L, "+998901112233", true);

        BusinessException exception = catchThrowableOfType(
                () -> facade.getSchedule(actor, LocalDate.of(2026, 8, 25), LocalDate.of(2026, 8, 18)),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void givenTechnicianActor_whenAcceptAssignment_thenDelegatesToService() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 17L, "+998901112233", true);
        RepairRequest req = new RepairRequest("REQ-2026-000042", customer, category, "Problem", "Chilanzar", null, null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        ReflectionTestUtils.setField(req, "id", 42L);

        RepairAssignment assignment = new RepairAssignment(req, technician, NOW.plusDays(1), admin, NOW);
        assignment.accept(NOW);
        ReflectionTestUtils.setField(assignment, "id", 19L);

        when(accessPolicy.requireCurrentTechnicianCanReadRequest(actor, 42L)).thenReturn(req);
        when(repairAssignmentRepository.findByRepairRequestIdAndTechnicianIdAndStatusInOrderByCreatedAtDesc(eq(42L), eq(17L), any()))
                .thenReturn(List.of(assignment));

        facade.acceptAssignment(actor, 42L);

        verify(repairAssignmentService).acceptByTechnician(42L, 17L);
    }

    @Test
    void givenTechnicianActor_whenRejectAssignment_thenDelegatesToService() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 17L, "+998901112233", true);
        AssignmentRejectionRequest request = new AssignmentRejectionRequest("Cannot reach location");

        facade.rejectAssignment(actor, 42L, request);

        verify(repairAssignmentService).rejectByTechnician(42L, request, 17L);
    }

    @Test
    void givenCustomerActor_whenCallingTechnicianFacadeMethods_thenThrowsAccessDenied() {
        AuthenticatedMobileActor customerActor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 101L, "+998901234567", true);

        BusinessException exception = catchThrowableOfType(
                () -> facade.getJobDetail(customerActor, 42L),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
    }
}
