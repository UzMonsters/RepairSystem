package com.example.darks.repair_auto.repair.execution.application;

import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.notification.application.NotificationEventFactory;
import com.example.darks.repair_auto.notification.application.NotificationOutboxService;
import com.example.darks.repair_auto.notification.domain.NotificationType;
import com.example.darks.repair_auto.repair.assignment.domain.AssignmentStatus;
import com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.attachment.application.AttachmentRequirementService;
import com.example.darks.repair_auto.repair.execution.api.dto.CancelRepairRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.CompleteRepairRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.DiagnosisRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.RepairExecutionDetailResponse;
import com.example.darks.repair_auto.repair.execution.api.dto.RepairExecutionMapper;
import com.example.darks.repair_auto.repair.execution.api.dto.RepairRequestStatusHistoryResponse;
import com.example.darks.repair_auto.repair.execution.api.dto.ResumeRepairRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.WaitForPartsRequest;
import com.example.darks.repair_auto.repair.execution.domain.RepairExecution;
import com.example.darks.repair_auto.repair.execution.domain.RepairRequestStatusHistory;
import com.example.darks.repair_auto.repair.execution.infrastructure.RepairExecutionRepository;
import com.example.darks.repair_auto.repair.execution.infrastructure.RepairRequestStatusHistoryRepository;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestDetailResponse;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestMapper;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.darks.repair_auto.localization.application.LocalizedValueResolver;
import com.example.darks.repair_auto.localization.infrastructure.EffectiveLanguageResolver;
import com.example.darks.repair_auto.realtime.event.application.RequestDiagnosisUpdatedDomainEvent;
import com.example.darks.repair_auto.realtime.event.application.RequestStatusChangedDomainEvent;
import com.example.darks.repair_auto.settings.domain.Language;
import org.springframework.context.ApplicationEventPublisher;

@Service
public class RepairExecutionService {

    private static final int MAX_DIAGNOSIS_LENGTH = 4000;
    private static final int MAX_WORK_PERFORMED_LENGTH = 4000;
    private static final int MAX_COMPLETION_NOTE_LENGTH = 2000;
    private static final int MAX_REASON_LENGTH = 1000;

    private final RepairRequestRepository repairRequestRepository;
    private final RepairAssignmentRepository repairAssignmentRepository;
    private final RepairExecutionRepository repairExecutionRepository;
    private final RepairRequestStatusHistoryRepository statusHistoryRepository;
    private final UserRepository userRepository;
    private final TechnicianRepository technicianRepository;
    private final RepairStatusHistoryService statusHistoryService;
    private final AttachmentRequirementService attachmentRequirementService;
    private final NotificationEventFactory notificationEventFactory;
    private final NotificationOutboxService notificationOutboxService;
    private final EffectiveLanguageResolver effectiveLanguageResolver;
    private final LocalizedValueResolver localizedValueResolver;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Clock clock;

    @Autowired
    public RepairExecutionService(
            RepairRequestRepository repairRequestRepository,
            RepairAssignmentRepository repairAssignmentRepository,
            RepairExecutionRepository repairExecutionRepository,
            RepairRequestStatusHistoryRepository statusHistoryRepository,
            UserRepository userRepository,
            TechnicianRepository technicianRepository,
            RepairStatusHistoryService statusHistoryService,
            AttachmentRequirementService attachmentRequirementService,
            NotificationEventFactory notificationEventFactory,
            NotificationOutboxService notificationOutboxService,
            EffectiveLanguageResolver effectiveLanguageResolver,
            LocalizedValueResolver localizedValueResolver,
            ApplicationEventPublisher applicationEventPublisher) {
        this(
                repairRequestRepository,
                repairAssignmentRepository,
                repairExecutionRepository,
                statusHistoryRepository,
                userRepository,
                technicianRepository,
                statusHistoryService,
                attachmentRequirementService,
                notificationEventFactory,
                notificationOutboxService,
                effectiveLanguageResolver,
                localizedValueResolver,
                applicationEventPublisher,
                Clock.systemUTC());
    }

    public RepairExecutionService(
            RepairRequestRepository repairRequestRepository,
            RepairAssignmentRepository repairAssignmentRepository,
            RepairExecutionRepository repairExecutionRepository,
            RepairRequestStatusHistoryRepository statusHistoryRepository,
            UserRepository userRepository,
            TechnicianRepository technicianRepository,
            RepairStatusHistoryService statusHistoryService,
            AttachmentRequirementService attachmentRequirementService,
            NotificationEventFactory notificationEventFactory,
            NotificationOutboxService notificationOutboxService,
            EffectiveLanguageResolver effectiveLanguageResolver,
            LocalizedValueResolver localizedValueResolver,
            Clock clock) {
        this(
                repairRequestRepository,
                repairAssignmentRepository,
                repairExecutionRepository,
                statusHistoryRepository,
                userRepository,
                technicianRepository,
                statusHistoryService,
                attachmentRequirementService,
                notificationEventFactory,
                notificationOutboxService,
                effectiveLanguageResolver,
                localizedValueResolver,
                null,
                clock);
    }

    public RepairExecutionService(
            RepairRequestRepository repairRequestRepository,
            RepairAssignmentRepository repairAssignmentRepository,
            RepairExecutionRepository repairExecutionRepository,
            RepairRequestStatusHistoryRepository statusHistoryRepository,
            UserRepository userRepository,
            TechnicianRepository technicianRepository,
            RepairStatusHistoryService statusHistoryService,
            AttachmentRequirementService attachmentRequirementService,
            NotificationEventFactory notificationEventFactory,
            NotificationOutboxService notificationOutboxService,
            EffectiveLanguageResolver effectiveLanguageResolver,
            LocalizedValueResolver localizedValueResolver,
            ApplicationEventPublisher applicationEventPublisher,
            Clock clock) {
        this.repairRequestRepository = repairRequestRepository;
        this.repairAssignmentRepository = repairAssignmentRepository;
        this.repairExecutionRepository = repairExecutionRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.userRepository = userRepository;
        this.technicianRepository = technicianRepository;
        this.statusHistoryService = statusHistoryService;
        this.attachmentRequirementService = attachmentRequirementService;
        this.notificationEventFactory = notificationEventFactory;
        this.notificationOutboxService = notificationOutboxService;
        this.effectiveLanguageResolver = effectiveLanguageResolver;
        this.localizedValueResolver = localizedValueResolver;
        this.applicationEventPublisher = applicationEventPublisher;
        this.clock = clock;
    }

    @Transactional
    public RepairRequestDetailResponse start(Long requestId, AuthenticatedUser user) {
        OffsetDateTime now = now();
        User changedBy = user(user);
        RepairRequest request = requestForUpdate(requestId);
        RepairRequestStatus fromStatus = request.getStatus();
        if (fromStatus != RepairRequestStatus.ASSIGNED && fromStatus != RepairRequestStatus.SCHEDULED) {
            throw stateConflict("REPAIR_NOT_STARTABLE", "Repair can start only from ASSIGNED or SCHEDULED.");
        }
        RepairAssignment assignment = activeAcceptedAssignment(requestId);
        if (!assignment.getTechnician().isActive()) {
            throw stateConflict("TECHNICIAN_INACTIVE", "Inactive technician cannot start repair work.");
        }
        RepairExecution execution = executionForUpdateOrCreate(request, now);
        if (execution.hasStarted()) {
            throw stateConflict("REPAIR_ALREADY_STARTED", "Repair execution has already started.");
        }
        execution.start(changedBy, now);
        request.markInProgress(now);
        saveExecution(execution);
        var history = statusHistoryService.recordTransition(request, fromStatus, "Repair work started.", changedBy, now);
        enqueueCustomerStatus(NotificationType.REPAIR_STARTED, request, history);
        publishDomainEvent(new RequestStatusChangedDomainEvent(
                request.getId(),
                request.getRequestNumber(),
                request.getCustomer().getId(),
                assignment.getTechnician().getId(),
                fromStatus,
                request.getStatus()));
        return details(request, assignment, execution);
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public RepairRequestDetailResponse startByTechnician(Long requestId, Long technicianId) {
        OffsetDateTime now = now();
        Technician changedBy = technician(technicianId);
        RepairRequest request = requestForUpdate(requestId);
        RepairRequestStatus fromStatus = request.getStatus();
        if (fromStatus != RepairRequestStatus.ASSIGNED && fromStatus != RepairRequestStatus.SCHEDULED) {
            throw stateConflict("REPAIR_NOT_STARTABLE", "Repair can start only from ASSIGNED or SCHEDULED.");
        }
        RepairAssignment assignment = activeAcceptedAssignment(requestId);
        requireTechnicianAssignment(assignment, changedBy);
        if (!assignment.getTechnician().isActive()) {
            throw stateConflict("TECHNICIAN_INACTIVE", "Inactive technician cannot start repair work.");
        }
        RepairExecution execution = executionForUpdateOrCreate(request, now);
        if (execution.hasStarted()) {
            throw stateConflict("REPAIR_ALREADY_STARTED", "Repair execution has already started.");
        }
        execution.startByTechnician(changedBy, now);
        request.markInProgress(now);
        saveExecution(execution);
        var history = statusHistoryService.recordTransition(request, fromStatus, "Repair work started.", changedBy, now);
        enqueueCustomerStatus(NotificationType.REPAIR_STARTED, request, history);
        publishDomainEvent(new RequestStatusChangedDomainEvent(
                request.getId(),
                request.getRequestNumber(),
                request.getCustomer().getId(),
                assignment.getTechnician().getId(),
                fromStatus,
                request.getStatus()));
        return details(request, assignment, execution);
    }

    @Transactional
    public RepairExecutionDetailResponse updateDiagnosis(
            Long requestId,
            DiagnosisRequest requestBody,
            AuthenticatedUser user) {
        OffsetDateTime now = now();
        User changedBy = user(user);
        RepairRequest request = requestForUpdate(requestId);
        requireStatus(
                request,
                "INVALID_REPAIR_STATUS_TRANSITION",
                RepairRequestStatus.IN_PROGRESS,
                RepairRequestStatus.WAITING_FOR_PARTS);
        RepairExecution execution = existingExecutionForUpdate(requestId);
        execution.updateDiagnosis(validateText(
                requestBody.diagnosis(),
                MAX_DIAGNOSIS_LENGTH,
                "INVALID_DIAGNOSIS",
                "Diagnosis must be between 1 and 4000 characters.",
                false), changedBy, now);
        RepairExecution saved = saveExecution(execution);
        Long techId = repairAssignmentRepository.findActiveByRequestId(requestId, RepairAssignmentRepository.ACTIVE_STATUSES)
                .map(a -> a.getTechnician().getId())
                .orElse(null);
        publishDomainEvent(new RequestDiagnosisUpdatedDomainEvent(
                request.getId(),
                request.getRequestNumber(),
                saved.getId(),
                techId,
                request.getCustomer().getId()));
        return RepairExecutionMapper.details(saved);
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public RepairExecutionDetailResponse updateDiagnosisByTechnician(
            Long requestId,
            DiagnosisRequest requestBody,
            Long technicianId) {
        OffsetDateTime now = now();
        Technician changedBy = technician(technicianId);
        RepairRequest request = requestForUpdate(requestId);
        requireStatus(
                request,
                "INVALID_REPAIR_STATUS_TRANSITION",
                RepairRequestStatus.IN_PROGRESS,
                RepairRequestStatus.WAITING_FOR_PARTS);
        requireTechnicianAssignment(activeAcceptedAssignment(requestId), changedBy);
        RepairExecution execution = existingExecutionForUpdate(requestId);
        execution.updateDiagnosisByTechnician(validateText(
                requestBody.diagnosis(),
                MAX_DIAGNOSIS_LENGTH,
                "INVALID_DIAGNOSIS",
                "Diagnosis must be between 1 and 4000 characters.",
                false), changedBy, now);
        RepairExecution saved = saveExecution(execution);
        publishDomainEvent(new RequestDiagnosisUpdatedDomainEvent(
                request.getId(),
                request.getRequestNumber(),
                saved.getId(),
                changedBy.getId(),
                request.getCustomer().getId()));
        return RepairExecutionMapper.details(saved);
    }

    @Transactional
    public RepairRequestDetailResponse waitForParts(
            Long requestId,
            WaitForPartsRequest requestBody,
            AuthenticatedUser user) {
        OffsetDateTime now = now();
        User changedBy = user(user);
        RepairRequest request = requestForUpdate(requestId);
        RepairRequestStatus fromStatus = request.getStatus();
        requireStatus(request, "REPAIR_NOT_IN_PROGRESS", RepairRequestStatus.IN_PROGRESS);
        RepairAssignment assignment = activeAcceptedAssignment(requestId);
        RepairExecution execution = startedExecutionForUpdate(requestId);
        String reason = validateText(
                requestBody.reason(),
                MAX_REASON_LENGTH,
                "INVALID_WAITING_REASON",
                "Waiting reason must be between 1 and 1000 characters.",
                false);
        execution.waitForParts(reason, now);
        request.markWaitingForParts(now);
        saveExecution(execution);
        var history = statusHistoryService.recordTransition(request, fromStatus, reason, changedBy, now);
        enqueueCustomerStatus(NotificationType.WAITING_FOR_PARTS, request, history);
        publishDomainEvent(new RequestStatusChangedDomainEvent(
                request.getId(),
                request.getRequestNumber(),
                request.getCustomer().getId(),
                assignment.getTechnician().getId(),
                fromStatus,
                request.getStatus()));
        return details(request, assignment, execution);
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public RepairRequestDetailResponse waitForPartsByTechnician(
            Long requestId,
            WaitForPartsRequest requestBody,
            Long technicianId) {
        OffsetDateTime now = now();
        Technician changedBy = technician(technicianId);
        RepairRequest request = requestForUpdate(requestId);
        RepairRequestStatus fromStatus = request.getStatus();
        requireStatus(request, "REPAIR_NOT_IN_PROGRESS", RepairRequestStatus.IN_PROGRESS);
        RepairAssignment assignment = activeAcceptedAssignment(requestId);
        requireTechnicianAssignment(assignment, changedBy);
        RepairExecution execution = startedExecutionForUpdate(requestId);
        String reason = validateText(
                requestBody.reason(),
                MAX_REASON_LENGTH,
                "INVALID_WAITING_REASON",
                "Waiting reason must be between 1 and 1000 characters.",
                false);
        execution.waitForParts(reason, now);
        request.markWaitingForParts(now);
        saveExecution(execution);
        var history = statusHistoryService.recordTransition(request, fromStatus, reason, changedBy, now);
        enqueueCustomerStatus(NotificationType.WAITING_FOR_PARTS, request, history);
        publishDomainEvent(new RequestStatusChangedDomainEvent(
                request.getId(),
                request.getRequestNumber(),
                request.getCustomer().getId(),
                assignment.getTechnician().getId(),
                fromStatus,
                request.getStatus()));
        return details(request, assignment, execution);
    }

    @Transactional
    public RepairRequestDetailResponse resume(Long requestId, ResumeRepairRequest requestBody, AuthenticatedUser user) {
        OffsetDateTime now = now();
        User changedBy = user(user);
        RepairRequest request = requestForUpdate(requestId);
        RepairRequestStatus fromStatus = request.getStatus();
        requireStatus(request, "REPAIR_NOT_WAITING_FOR_PARTS", RepairRequestStatus.WAITING_FOR_PARTS);
        RepairAssignment assignment = activeAcceptedAssignment(requestId);
        RepairExecution execution = startedExecutionForUpdate(requestId);
        String previousReason = execution.clearWaiting(now);
        request.markInProgress(now);
        saveExecution(execution);
        var history = statusHistoryService.recordTransition(
                request,
                fromStatus,
                resumeReason(previousReason, requestBody == null ? null : requestBody.note()),
                changedBy,
                now);
        enqueueCustomerStatus(NotificationType.REPAIR_RESUMED, request, history);
        publishDomainEvent(new RequestStatusChangedDomainEvent(
                request.getId(),
                request.getRequestNumber(),
                request.getCustomer().getId(),
                assignment.getTechnician().getId(),
                fromStatus,
                request.getStatus()));
        return details(request, assignment, execution);
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public RepairRequestDetailResponse resumeByTechnician(
            Long requestId,
            ResumeRepairRequest requestBody,
            Long technicianId) {
        OffsetDateTime now = now();
        Technician changedBy = technician(technicianId);
        RepairRequest request = requestForUpdate(requestId);
        RepairRequestStatus fromStatus = request.getStatus();
        requireStatus(request, "REPAIR_NOT_WAITING_FOR_PARTS", RepairRequestStatus.WAITING_FOR_PARTS);
        RepairAssignment assignment = activeAcceptedAssignment(requestId);
        requireTechnicianAssignment(assignment, changedBy);
        RepairExecution execution = startedExecutionForUpdate(requestId);
        String previousReason = execution.clearWaiting(now);
        request.markInProgress(now);
        saveExecution(execution);
        var history = statusHistoryService.recordTransition(
                request,
                fromStatus,
                resumeReason(previousReason, requestBody == null ? null : requestBody.note()),
                changedBy,
                now);
        enqueueCustomerStatus(NotificationType.REPAIR_RESUMED, request, history);
        publishDomainEvent(new RequestStatusChangedDomainEvent(
                request.getId(),
                request.getRequestNumber(),
                request.getCustomer().getId(),
                assignment.getTechnician().getId(),
                fromStatus,
                request.getStatus()));
        return details(request, assignment, execution);
    }

    @Transactional
    public RepairRequestDetailResponse complete(
            Long requestId,
            CompleteRepairRequest requestBody,
            AuthenticatedUser user) {
        OffsetDateTime now = now();
        User changedBy = user(user);
        RepairRequest request = requestForUpdate(requestId);
        RepairRequestStatus fromStatus = request.getStatus();
        if (fromStatus == RepairRequestStatus.COMPLETED) {
            throw stateConflict("REPAIR_ALREADY_COMPLETED", "Repair request is already completed.");
        }
        if (fromStatus == RepairRequestStatus.CANCELLED) {
            throw stateConflict("REPAIR_ALREADY_CANCELLED", "Repair request is already cancelled.");
        }
        requireStatus(request, "REPAIR_NOT_IN_PROGRESS", RepairRequestStatus.IN_PROGRESS);
        RepairAssignment assignment = activeAcceptedAssignment(requestId);
        RepairExecution execution = startedExecutionForUpdate(requestId);
        if (!execution.hasDiagnosis()) {
            throw stateConflict("DIAGNOSIS_REQUIRED", "Diagnosis is required before completion.");
        }
        String workPerformed = validateText(
                requestBody.workPerformed(),
                MAX_WORK_PERFORMED_LENGTH,
                "WORK_PERFORMED_REQUIRED",
                "Work performed must be between 1 and 4000 characters.",
                false);
        String completionNote = validateText(
                requestBody.completionNote(),
                MAX_COMPLETION_NOTE_LENGTH,
                "VALIDATION_FAILED",
                "Completion note must be at most 2000 characters.",
                true);
        attachmentRequirementService.requireAvailableCompletionPhoto(requestId);
        execution.complete(workPerformed, completionNote, changedBy, now);
        assignment.complete(now);
        request.markCompleted(now);
        saveExecution(execution);
        var history = statusHistoryService.recordTransition(request, fromStatus, "Repair completed.", changedBy, now);
        enqueueCustomerStatus(NotificationType.REPAIR_COMPLETED, request, history);
        publishDomainEvent(new RequestStatusChangedDomainEvent(
                request.getId(),
                request.getRequestNumber(),
                request.getCustomer().getId(),
                assignment.getTechnician().getId(),
                fromStatus,
                request.getStatus()));
        return details(request, null, execution);
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public RepairRequestDetailResponse completeByTechnician(
            Long requestId,
            CompleteRepairRequest requestBody,
            Long technicianId) {
        OffsetDateTime now = now();
        Technician changedBy = technician(technicianId);
        RepairRequest request = requestForUpdate(requestId);
        RepairRequestStatus fromStatus = request.getStatus();
        if (fromStatus == RepairRequestStatus.COMPLETED) {
            throw stateConflict("REPAIR_ALREADY_COMPLETED", "Repair request is already completed.");
        }
        if (fromStatus == RepairRequestStatus.CANCELLED) {
            throw stateConflict("REPAIR_ALREADY_CANCELLED", "Repair request is already cancelled.");
        }
        requireStatus(request, "REPAIR_NOT_IN_PROGRESS", RepairRequestStatus.IN_PROGRESS);
        RepairAssignment assignment = activeAcceptedAssignment(requestId);
        requireTechnicianAssignment(assignment, changedBy);
        RepairExecution execution = startedExecutionForUpdate(requestId);
        if (!execution.hasDiagnosis()) {
            throw stateConflict("DIAGNOSIS_REQUIRED", "Diagnosis is required before completion.");
        }
        String workPerformed = validateText(
                requestBody.workPerformed(),
                MAX_WORK_PERFORMED_LENGTH,
                "WORK_PERFORMED_REQUIRED",
                "Work performed must be between 1 and 4000 characters.",
                false);
        String completionNote = validateText(
                requestBody.completionNote(),
                MAX_COMPLETION_NOTE_LENGTH,
                "VALIDATION_FAILED",
                "Completion note must be at most 2000 characters.",
                true);
        attachmentRequirementService.requireAvailableCompletionPhoto(requestId);
        execution.completeByTechnician(workPerformed, completionNote, changedBy, now);
        assignment.complete(now);
        request.markCompleted(now);
        saveExecution(execution);
        var history = statusHistoryService.recordTransition(request, fromStatus, "Repair completed.", changedBy, now);
        enqueueCustomerStatus(NotificationType.REPAIR_COMPLETED, request, history);
        publishDomainEvent(new RequestStatusChangedDomainEvent(
                request.getId(),
                request.getRequestNumber(),
                request.getCustomer().getId(),
                assignment.getTechnician().getId(),
                fromStatus,
                request.getStatus()));
        return details(request, null, execution);
    }

    @Transactional
    public RepairRequestDetailResponse cancel(Long requestId, CancelRepairRequest requestBody, AuthenticatedUser user) {
        OffsetDateTime now = now();
        User changedBy = user(user);
        RepairRequest request = requestForUpdate(requestId);
        RepairRequestStatus fromStatus = request.getStatus();
        if (fromStatus == RepairRequestStatus.COMPLETED) {
            throw stateConflict("REPAIR_ALREADY_COMPLETED", "Completed repair requests cannot be cancelled.");
        }
        if (fromStatus == RepairRequestStatus.CANCELLED) {
            throw stateConflict("REPAIR_ALREADY_CANCELLED", "Repair request is already cancelled.");
        }
        if (fromStatus != RepairRequestStatus.NEW
                && fromStatus != RepairRequestStatus.ASSIGNED
                && fromStatus != RepairRequestStatus.SCHEDULED
                && fromStatus != RepairRequestStatus.IN_PROGRESS
                && fromStatus != RepairRequestStatus.WAITING_FOR_PARTS) {
            throw stateConflict("INVALID_REPAIR_STATUS_TRANSITION", "Repair request cannot be cancelled now.");
        }
        String reason = validateText(
                requestBody.reason(),
                MAX_REASON_LENGTH,
                "INVALID_CANCELLATION_REASON",
                "Cancellation reason must be between 1 and 1000 characters.",
                false);
        RepairAssignment assignment = activeAssignmentForUpdate(requestId);
        RepairExecution execution = executionForUpdateOrCreate(request, now);
        execution.cancel(reason, changedBy, now);
        if (assignment != null) {
            assignment.cancel(reason, now);
        }
        request.markCancelled(now);
        saveExecution(execution);
        var history = statusHistoryService.recordTransition(request, fromStatus, reason, changedBy, now);
        enqueueCustomerStatus(NotificationType.REQUEST_CANCELLED, request, history);
        Long techId = assignment != null ? assignment.getTechnician().getId() : null;
        if (assignment != null) {
            notificationOutboxService.enqueue(notificationEventFactory.technician(
                    NotificationType.REQUEST_CANCELLED,
                    request,
                    assignment.getTechnician(),
                    assignment.getScheduledVisitAt(),
                    NotificationEventFactory.statusEventKeyPart(history.getId(), request.getStatus())));
        }
        publishDomainEvent(new RequestStatusChangedDomainEvent(
                request.getId(),
                request.getRequestNumber(),
                request.getCustomer().getId(),
                techId,
                fromStatus,
                request.getStatus()));
        return details(request, null, execution);
    }

    private void publishDomainEvent(Object event) {
        if (applicationEventPublisher != null) {
            applicationEventPublisher.publishEvent(event);
        }
    }

    @Transactional(readOnly = true)
    public RepairExecutionDetailResponse getExecution(Long requestId) {
        repairRequestRepository.findWithRelationsById(requestId).orElseThrow(this::requestNotFound);
        return RepairExecutionMapper.details(repairExecutionRepository
                .findByRepairRequestId(requestId)
                .orElseThrow(this::executionNotFound));
    }

    @Transactional(readOnly = true)
    public List<RepairRequestStatusHistoryResponse> statusHistory(Long requestId) {
        repairRequestRepository.findWithRelationsById(requestId).orElseThrow(this::requestNotFound);
        Language lang = effectiveLanguageResolver.resolveEffectiveLanguage();
        return statusHistoryRepository.findByRepairRequestIdOrderByChangedAtDescIdDesc(requestId)
                .stream()
                .map(history -> RepairExecutionMapper.history(history, localizedHistoryReason(history.getReason(), lang)))
                .toList();
    }

    private String localizedHistoryReason(String reason, Language lang) {
        if (reason == null || lang == null) {
            return reason;
        }
        return switch (reason) {
            case "Request created." -> switch (lang) {
                case EN -> "Request created.";
                case RU -> "Заявка создана.";
                case UZ -> "Ariza yaratildi.";
            };
            case "Telegram request created." -> switch (lang) {
                case EN -> "Telegram request created.";
                case RU -> "Заявка создана через Telegram.";
                case UZ -> "Ariza Telegram orqali yaratildi.";
            };
            case "Mobile request created." -> switch (lang) {
                case EN -> "Mobile request created.";
                case RU -> "Заявка создана через мобильное приложение.";
                case UZ -> "Ariza mobil ilova orqali yaratildi.";
            };
            case "Technician assigned." -> switch (lang) {
                case EN -> "Technician assigned.";
                case RU -> "Техник назначен.";
                case UZ -> "Texnik tayinlandi.";
            };
            case "Schedule changed." -> switch (lang) {
                case EN -> "Schedule changed.";
                case RU -> "График изменен.";
                case UZ -> "Jadval o'zgartirildi.";
            };
            case "Repair work started." -> switch (lang) {
                case EN -> "Repair work started.";
                case RU -> "Ремонт начат.";
                case UZ -> "Ta'mirlash boshlandi.";
            };
            case "Repair completed." -> switch (lang) {
                case EN -> "Repair completed.";
                case RU -> "Ремонт завершен.";
                case UZ -> "Ta'mirlash yakunlandi.";
            };
            default -> localizedResumeReason(reason, lang);
        };
    }

    private String localizedResumeReason(String reason, Language lang) {
        String prefix = "Resumed from waiting for parts. Previous reason: ";
        if (reason.startsWith(prefix)) {
            String previousReason = reason.substring(prefix.length());
            return switch (lang) {
                case EN -> reason;
                case RU -> "Ремонт возобновлен после ожидания запчастей. Предыдущая причина: " + previousReason;
                case UZ -> "Ehtiyot qismlar kutilganidan keyin ta'mirlash davom ettirildi. Oldingi sabab: "
                        + previousReason;
            };
        }
        String marker = " Previous waiting reason: ";
        int markerIndex = reason.indexOf(marker);
        if (markerIndex >= 0) {
            String note = reason.substring(0, markerIndex);
            String previousReason = reason.substring(markerIndex + marker.length());
            return switch (lang) {
                case EN -> reason;
                case RU -> note + " Предыдущая причина ожидания: " + previousReason;
                case UZ -> note + " Oldingi kutish sababi: " + previousReason;
            };
        }
        return reason;
    }

    private RepairRequestDetailResponse details(
            RepairRequest request,
            RepairAssignment assignment,
            RepairExecution execution) {
        Language lang = effectiveLanguageResolver.resolveEffectiveLanguage();
        return RepairRequestMapper.details(request, assignment, execution, lang, localizedValueResolver);
    }

    private RepairRequest requestForUpdate(Long requestId) {
        return repairRequestRepository.findByIdForUpdate(requestId).orElseThrow(this::requestNotFound);
    }

    private RepairAssignment activeAssignmentForUpdate(Long requestId) {
        return repairAssignmentRepository
                .findActiveByRequestIdForUpdate(requestId, RepairAssignmentRepository.ACTIVE_STATUSES)
                .orElse(null);
    }

    private RepairAssignment activeAcceptedAssignment(Long requestId) {
        RepairAssignment assignment = activeAssignmentForUpdate(requestId);
        if (assignment == null || assignment.getStatus() != AssignmentStatus.ACCEPTED) {
            throw stateConflict(
                    "ACTIVE_ACCEPTED_ASSIGNMENT_REQUIRED",
                    "An active accepted assignment is required for this repair action.");
        }
        return assignment;
    }

    private RepairExecution executionForUpdateOrCreate(RepairRequest request, OffsetDateTime now) {
        return repairExecutionRepository.findByRepairRequestIdForUpdate(request.getId())
                .orElseGet(() -> new RepairExecution(request, now));
    }

    private RepairExecution existingExecutionForUpdate(Long requestId) {
        return repairExecutionRepository.findByRepairRequestIdForUpdate(requestId)
                .orElseThrow(this::executionNotFound);
    }

    private RepairExecution startedExecutionForUpdate(Long requestId) {
        RepairExecution execution = existingExecutionForUpdate(requestId);
        if (!execution.hasStarted()) {
            throw stateConflict("REPAIR_NOT_STARTABLE", "Repair execution has not started.");
        }
        return execution;
    }

    private RepairExecution saveExecution(RepairExecution execution) {
        try {
            return repairExecutionRepository.saveAndFlush(execution);
        } catch (DataIntegrityViolationException exception) {
            throw stateConflict(
                    "REPAIR_EXECUTION_CONFLICT",
                    "Repair execution changed concurrently. Reload and try again.");
        }
    }

    private void requireStatus(RepairRequest request, String code, RepairRequestStatus... allowedStatuses) {
        for (RepairRequestStatus status : allowedStatuses) {
            if (request.getStatus() == status) {
                return;
            }
        }
        throw stateConflict(code, "Repair request is not in a valid status for this action.");
    }

    private void enqueueCustomerStatus(
            NotificationType type,
            RepairRequest request,
            RepairRequestStatusHistory history) {
        notificationOutboxService.enqueue(notificationEventFactory.customer(
                type,
                request,
                NotificationEventFactory.statusEventKeyPart(
                        history == null ? null : history.getId(),
                        request.getStatus())));
    }

    private String validateText(String value, int maxLength, String code, String message, boolean optional) {
        if (value == null || value.isBlank()) {
            if (optional) {
                return null;
            }
            throw new BusinessRuleException(code, message, 400);
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new BusinessRuleException(code, message, 400);
        }
        return trimmed;
    }

    private String resumeReason(String previousWaitingReason, String note) {
        String trimmedNote = validateText(
                note,
                MAX_REASON_LENGTH,
                "VALIDATION_FAILED",
                "Resume note must be at most 1000 characters.",
                true);
        if (trimmedNote == null) {
            return "Resumed from waiting for parts. Previous reason: " + previousWaitingReason;
        }
        return trimmedNote + " Previous waiting reason: " + previousWaitingReason;
    }

    private User user(AuthenticatedUser user) {
        return userRepository.findById(user.id()).orElseThrow(this::userNotFound);
    }

    private Technician technician(Long technicianId) {
        Technician technician = technicianRepository.findByIdForUpdate(technicianId).orElseThrow(this::technicianNotFound);
        if (!technician.isActive()) {
            throw stateConflict("TECHNICIAN_INACTIVE", "Inactive technician cannot perform this action.");
        }
        return technician;
    }

    private void requireTechnicianAssignment(RepairAssignment assignment, Technician technician) {
        if (!assignment.getTechnician().getId().equals(technician.getId())) {
            throw stateConflict("TECHNICIAN_ASSIGNMENT_FORBIDDEN", "Repair request belongs to another technician.");
        }
    }

    private BusinessRuleException stateConflict(String code, String message) {
        return new BusinessRuleException(code, message, 409);
    }

    private BusinessRuleException requestNotFound() {
        return new BusinessRuleException("REPAIR_REQUEST_NOT_FOUND", "Repair request was not found.", 404);
    }

    private BusinessRuleException executionNotFound() {
        return new BusinessRuleException("REPAIR_EXECUTION_NOT_FOUND", "Repair execution was not found.", 404);
    }

    private BusinessRuleException userNotFound() {
        return new BusinessRuleException("USER_NOT_FOUND", "Authenticated user was not found.", 404);
    }

    private BusinessRuleException technicianNotFound() {
        return new BusinessRuleException("TECHNICIAN_NOT_FOUND", "Technician was not found.", 404);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }
}
