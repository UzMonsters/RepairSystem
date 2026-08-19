package com.example.darks.repair_auto.repair.technician.mobile.application;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.repair.access.application.RepairResourceAccessPolicy;
import com.example.darks.repair_auto.repair.action.application.RepairActionCapabilityService;
import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentRejectionRequest;
import com.example.darks.repair_auto.repair.assignment.application.RepairAssignmentService;
import com.example.darks.repair_auto.repair.assignment.domain.AssignmentStatus;
import com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.execution.api.dto.CompleteRepairRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.DiagnosisRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.ResumeRepairRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.WaitForPartsRequest;
import com.example.darks.repair_auto.repair.execution.application.RepairExecutionService;
import com.example.darks.repair_auto.repair.execution.domain.RepairExecution;
import com.example.darks.repair_auto.repair.execution.infrastructure.RepairExecutionRepository;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.repair.technician.mobile.api.dto.TechnicianJobDetailResponse;
import com.example.darks.repair_auto.repair.technician.mobile.api.dto.TechnicianJobListView;
import com.example.darks.repair_auto.repair.technician.mobile.api.dto.TechnicianJobSummaryResponse;
import com.example.darks.repair_auto.repair.technician.mobile.api.dto.TechnicianScheduleItemResponse;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.i18n.LocalizationService;
import com.example.darks.repair_auto.shared.i18n.RequestLocaleResolver;
import com.example.darks.repair_auto.shared.i18n.SupportedLanguage;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TechnicianJobFacade {

    private static final long MAX_SCHEDULE_RANGE_DAYS = 60;

    private static final List<AssignmentStatus> ACTIVE_VIEW_STATUSES = List.of(
            AssignmentStatus.PENDING,
            AssignmentStatus.ACCEPTED);

    private static final List<AssignmentStatus> HISTORY_VIEW_STATUSES = List.of(
            AssignmentStatus.COMPLETED,
            AssignmentStatus.CANCELLED);

    private final RepairAssignmentService repairAssignmentService;
    private final RepairExecutionService repairExecutionService;
    private final RepairResourceAccessPolicy accessPolicy;
    private final RepairAssignmentRepository repairAssignmentRepository;
    private final RepairExecutionRepository repairExecutionRepository;
    private final RepairActionCapabilityService actionCapabilityService;
    private final RequestLocaleResolver requestLocaleResolver;
    private final LocalizationService localizationService;

    public TechnicianJobFacade(
            RepairAssignmentService repairAssignmentService,
            RepairExecutionService repairExecutionService,
            RepairResourceAccessPolicy accessPolicy,
            RepairAssignmentRepository repairAssignmentRepository,
            RepairExecutionRepository repairExecutionRepository,
            RepairActionCapabilityService actionCapabilityService,
            RequestLocaleResolver requestLocaleResolver,
            LocalizationService localizationService) {
        this.repairAssignmentService = repairAssignmentService;
        this.repairExecutionService = repairExecutionService;
        this.accessPolicy = accessPolicy;
        this.repairAssignmentRepository = repairAssignmentRepository;
        this.repairExecutionRepository = repairExecutionRepository;
        this.actionCapabilityService = actionCapabilityService;
        this.requestLocaleResolver = requestLocaleResolver;
        this.localizationService = localizationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<TechnicianJobSummaryResponse> listJobs(
            AuthenticatedMobileActor actor,
            TechnicianJobListView view,
            Pageable pageable) {
        requireTechnician(actor);
        Collection<AssignmentStatus> statuses = (view == TechnicianJobListView.HISTORY)
                ? HISTORY_VIEW_STATUSES
                : ACTIVE_VIEW_STATUSES;

        Page<RepairAssignment> page = repairAssignmentRepository.findJobsByTechnicianIdAndStatusIn(
                actor.actorId(),
                statuses,
                pageable);

        return PageResponse.from(page.map(this::toSummaryResponse));
    }

    @Transactional(readOnly = true)
    public TechnicianJobDetailResponse getJobDetail(
            AuthenticatedMobileActor actor,
            Long requestId) {
        requireTechnician(actor);
        RepairRequest request = accessPolicy.requireCurrentTechnicianCanReadRequest(actor, requestId);

        List<RepairAssignment> assignments = repairAssignmentRepository
                .findByRepairRequestIdAndTechnicianIdAndStatusInOrderByCreatedAtDesc(
                        requestId,
                        actor.actorId(),
                        RepairResourceAccessPolicy.READABLE_TECHNICIAN_ASSIGNMENT_STATUSES);

        if (assignments.isEmpty()) {
            throw new BusinessException(ErrorCode.REPAIR_REQUEST_NOT_FOUND);
        }

        RepairAssignment assignment = assignments.get(0);
        RepairExecution execution = repairExecutionRepository.findByRepairRequestId(requestId).orElse(null);

        return toDetailResponse(request, assignment, execution);
    }

    @Transactional(readOnly = true)
    public List<TechnicianScheduleItemResponse> getSchedule(
            AuthenticatedMobileActor actor,
            LocalDate from,
            LocalDate to) {
        requireTechnician(actor);
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_SCHEDULE_RANGE_DAYS) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        OffsetDateTime fromOffset = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toOffset = to.atTime(23, 59, 59, 999999999).atOffset(ZoneOffset.UTC);

        List<RepairAssignment> assignments = repairAssignmentRepository.findSchedule(
                actor.actorId(),
                ACTIVE_VIEW_STATUSES,
                fromOffset,
                toOffset);

        return assignments.stream().map(this::toScheduleItemResponse).toList();
    }

    @Transactional
    public TechnicianJobDetailResponse acceptAssignment(AuthenticatedMobileActor actor, Long requestId) {
        requireTechnician(actor);
        repairAssignmentService.acceptByTechnician(requestId, actor.actorId());
        return getJobDetail(actor, requestId);
    }

    @Transactional
    public void rejectAssignment(
            AuthenticatedMobileActor actor,
            Long requestId,
            AssignmentRejectionRequest request) {
        requireTechnician(actor);
        repairAssignmentService.rejectByTechnician(requestId, request, actor.actorId());
    }

    @Transactional
    public TechnicianJobDetailResponse startRepair(AuthenticatedMobileActor actor, Long requestId) {
        requireTechnician(actor);
        repairExecutionService.startByTechnician(requestId, actor.actorId());
        return getJobDetail(actor, requestId);
    }

    @Transactional
    public TechnicianJobDetailResponse updateDiagnosis(
            AuthenticatedMobileActor actor,
            Long requestId,
            DiagnosisRequest request) {
        requireTechnician(actor);
        repairExecutionService.updateDiagnosisByTechnician(requestId, request, actor.actorId());
        return getJobDetail(actor, requestId);
    }

    @Transactional
    public TechnicianJobDetailResponse waitForParts(
            AuthenticatedMobileActor actor,
            Long requestId,
            WaitForPartsRequest request) {
        requireTechnician(actor);
        repairExecutionService.waitForPartsByTechnician(requestId, request, actor.actorId());
        return getJobDetail(actor, requestId);
    }

    @Transactional
    public TechnicianJobDetailResponse resumeRepair(
            AuthenticatedMobileActor actor,
            Long requestId,
            ResumeRepairRequest request) {
        requireTechnician(actor);
        repairExecutionService.resumeByTechnician(requestId, request, actor.actorId());
        return getJobDetail(actor, requestId);
    }

    @Transactional
    public TechnicianJobDetailResponse completeRepair(
            AuthenticatedMobileActor actor,
            Long requestId,
            CompleteRepairRequest request) {
        requireTechnician(actor);
        repairExecutionService.completeByTechnician(requestId, request, actor.actorId());
        return getJobDetail(actor, requestId);
    }

    private TechnicianJobSummaryResponse toSummaryResponse(RepairAssignment assignment) {
        SupportedLanguage language = requestLocaleResolver.resolveLanguage();
        RepairRequest request = assignment.getRepairRequest();
        Customer customer = request.getCustomer();

        return new TechnicianJobSummaryResponse(
                request.getId(),
                assignment.getId(),
                request.getRequestNumber(),
                request.getStatus(),
                localizeRequestStatus(request.getStatus(), language),
                assignment.getStatus(),
                localizeAssignmentStatus(assignment.getStatus(), language),
                new TechnicianJobSummaryResponse.CategorySummary(
                        request.getCategory().getId(),
                        localizeCategory(request.getCategory(), language)),
                new TechnicianJobSummaryResponse.CustomerSummary(customer.getFullName()),
                request.getAddress(),
                assignment.getScheduledVisitAt(),
                assignment.getCreatedAt());
    }

    private TechnicianJobDetailResponse toDetailResponse(
            RepairRequest request,
            RepairAssignment assignment,
            RepairExecution execution) {
        SupportedLanguage language = requestLocaleResolver.resolveLanguage();
        Customer customer = request.getCustomer();

        TechnicianJobDetailResponse.ScheduleInfo scheduleInfo = null;
        if (assignment.getScheduledVisitAt() != null) {
            scheduleInfo = new TechnicianJobDetailResponse.ScheduleInfo(assignment.getScheduledVisitAt());
        }

        TechnicianJobDetailResponse.ExecutionInfo executionInfo = null;
        if (execution != null) {
            executionInfo = new TechnicianJobDetailResponse.ExecutionInfo(
                    execution.getDiagnosis(),
                    execution.getWorkPerformed(),
                    execution.getCompletionNote(),
                    execution.getWaitingReason());
        }

        return new TechnicianJobDetailResponse(
                request.getId(),
                assignment.getId(),
                request.getRequestNumber(),
                request.getStatus(),
                localizeRequestStatus(request.getStatus(), language),
                assignment.getStatus(),
                localizeAssignmentStatus(assignment.getStatus(), language),
                new TechnicianJobDetailResponse.CategorySummary(
                        request.getCategory().getId(),
                        localizeCategory(request.getCategory(), language)),
                request.getDescription(),
                new TechnicianJobDetailResponse.CustomerInfo(
                        customer.getId(),
                        customer.getFullName(),
                        customer.getPhone()),
                new TechnicianJobDetailResponse.LocationInfo(
                        request.getLocationAddress(),
                        request.getLocationLatitude(),
                        request.getLocationLongitude(),
                        request.getLocationSource()),
                scheduleInfo,
                executionInfo,
                actionCapabilityService.resolveTechnicianActions(request, assignment, execution),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }

    private TechnicianScheduleItemResponse toScheduleItemResponse(RepairAssignment assignment) {
        SupportedLanguage language = requestLocaleResolver.resolveLanguage();
        RepairRequest request = assignment.getRepairRequest();
        Customer customer = request.getCustomer();

        return new TechnicianScheduleItemResponse(
                request.getId(),
                assignment.getId(),
                request.getRequestNumber(),
                request.getStatus(),
                localizeRequestStatus(request.getStatus(), language),
                assignment.getStatus(),
                localizeAssignmentStatus(assignment.getStatus(), language),
                new TechnicianScheduleItemResponse.CategorySummary(
                        request.getCategory().getId(),
                        localizeCategory(request.getCategory(), language)),
                new TechnicianScheduleItemResponse.CustomerSummary(
                        customer.getFullName(),
                        customer.getPhone()),
                request.getAddress(),
                assignment.getScheduledVisitAt());
    }

    private String localizeCategory(RepairCategory category, SupportedLanguage language) {
        if (category == null) {
            return "";
        }
        return switch (language) {
            case EN -> category.getNameEn();
            case RU -> category.getNameRu();
            case UZ -> category.getNameUz();
        };
    }

    private String localizeRequestStatus(RepairRequestStatus status, SupportedLanguage language) {
        if (status == null) {
            return "";
        }
        String key = "repair.status." + status.name().toLowerCase(Locale.ROOT).replace('_', '-');
        return localizationService.get(key, language);
    }

    private String localizeAssignmentStatus(AssignmentStatus status, SupportedLanguage language) {
        if (status == null) {
            return "";
        }
        String key = "repair.assignment-status." + status.name().toLowerCase(Locale.ROOT).replace('_', '-');
        return localizationService.get(key, language);
    }

    private void requireTechnician(AuthenticatedMobileActor actor) {
        if (actor == null || !actor.isTechnician()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (!actor.active()) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }
    }
}
