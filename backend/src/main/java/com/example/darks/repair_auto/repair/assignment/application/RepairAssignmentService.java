package com.example.darks.repair_auto.repair.assignment.application;

import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.notification.application.NotificationEventFactory;
import com.example.darks.repair_auto.notification.application.NotificationOutboxService;
import com.example.darks.repair_auto.notification.domain.NotificationType;
import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentDetailResponse;
import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentMapper;
import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentRejectionRequest;
import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentRequest;
import com.example.darks.repair_auto.repair.assignment.api.dto.ReassignmentRequest;
import com.example.darks.repair_auto.repair.assignment.api.dto.ScheduleRequest;
import com.example.darks.repair_auto.repair.assignment.api.dto.TechnicianWorkloadResponse;
import com.example.darks.repair_auto.repair.assignment.api.dto.UnassignmentRequest;
import com.example.darks.repair_auto.repair.assignment.domain.AssignmentStatus;
import com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.execution.application.RepairStatusHistoryService;
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
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.darks.repair_auto.localization.application.LocalizedValueResolver;
import com.example.darks.repair_auto.localization.infrastructure.EffectiveLanguageResolver;
import com.example.darks.repair_auto.settings.domain.Language;
import com.example.darks.repair_auto.chat.application.ChatService;
import com.example.darks.repair_auto.realtime.event.application.RequestAssignedDomainEvent;
import com.example.darks.repair_auto.realtime.event.application.RequestAssignmentAcceptedDomainEvent;
import com.example.darks.repair_auto.realtime.event.application.RequestAssignmentCreatedDomainEvent;
import com.example.darks.repair_auto.realtime.event.application.RequestAssignmentRejectedDomainEvent;
import com.example.darks.repair_auto.realtime.event.application.RequestReassignedDomainEvent;
import com.example.darks.repair_auto.realtime.event.application.RequestScheduleChangedDomainEvent;
import com.example.darks.repair_auto.realtime.event.application.RequestUnassignedDomainEvent;
import org.springframework.context.ApplicationEventPublisher;

@Service
public class RepairAssignmentService {

    private static final int MAX_REASON_LENGTH = 500;

    private final RepairAssignmentRepository repairAssignmentRepository;
    private final RepairRequestRepository repairRequestRepository;
    private final TechnicianRepository technicianRepository;
    private final UserRepository userRepository;
    private final RepairStatusHistoryService statusHistoryService;
    private final NotificationEventFactory notificationEventFactory;
    private final NotificationOutboxService notificationOutboxService;
    private final EffectiveLanguageResolver effectiveLanguageResolver;
    private final LocalizedValueResolver localizedValueResolver;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ChatService chatService;
    private final Clock clock;

    public RepairAssignmentService(
            RepairAssignmentRepository repairAssignmentRepository,
            RepairRequestRepository repairRequestRepository,
            TechnicianRepository technicianRepository,
            UserRepository userRepository,
            RepairStatusHistoryService statusHistoryService,
            NotificationEventFactory notificationEventFactory,
            NotificationOutboxService notificationOutboxService,
            EffectiveLanguageResolver effectiveLanguageResolver,
            LocalizedValueResolver localizedValueResolver,
            Clock clock) {
        this(
                repairAssignmentRepository,
                repairRequestRepository,
                technicianRepository,
                userRepository,
                statusHistoryService,
                notificationEventFactory,
                notificationOutboxService,
                effectiveLanguageResolver,
                localizedValueResolver,
                null,
                null,
                clock);
    }

    @Autowired
    public RepairAssignmentService(
            RepairAssignmentRepository repairAssignmentRepository,
            RepairRequestRepository repairRequestRepository,
            TechnicianRepository technicianRepository,
            UserRepository userRepository,
            RepairStatusHistoryService statusHistoryService,
            NotificationEventFactory notificationEventFactory,
            NotificationOutboxService notificationOutboxService,
            EffectiveLanguageResolver effectiveLanguageResolver,
            LocalizedValueResolver localizedValueResolver,
            ApplicationEventPublisher applicationEventPublisher,
            ChatService chatService) {
        this(
                repairAssignmentRepository,
                repairRequestRepository,
                technicianRepository,
                userRepository,
                statusHistoryService,
                notificationEventFactory,
                notificationOutboxService,
                effectiveLanguageResolver,
                localizedValueResolver,
                applicationEventPublisher,
                chatService,
                Clock.systemUTC());
    }

    public RepairAssignmentService(
            RepairAssignmentRepository repairAssignmentRepository,
            RepairRequestRepository repairRequestRepository,
            TechnicianRepository technicianRepository,
            UserRepository userRepository,
            RepairStatusHistoryService statusHistoryService,
            NotificationEventFactory notificationEventFactory,
            NotificationOutboxService notificationOutboxService,
            EffectiveLanguageResolver effectiveLanguageResolver,
            LocalizedValueResolver localizedValueResolver,
            ApplicationEventPublisher applicationEventPublisher,
            ChatService chatService,
            Clock clock) {
        this.repairAssignmentRepository = repairAssignmentRepository;
        this.repairRequestRepository = repairRequestRepository;
        this.technicianRepository = technicianRepository;
        this.userRepository = userRepository;
        this.statusHistoryService = statusHistoryService;
        this.notificationEventFactory = notificationEventFactory;
        this.notificationOutboxService = notificationOutboxService;
        this.effectiveLanguageResolver = effectiveLanguageResolver;
        this.localizedValueResolver = localizedValueResolver;
        this.applicationEventPublisher = applicationEventPublisher;
        this.chatService = chatService;
        this.clock = clock;
    }

    @Transactional
    public RepairRequestDetailResponse assign(Long requestId, AssignmentRequest request, AuthenticatedUser user) {
        OffsetDateTime now = now();
        RepairRequest repairRequest = assignableRequestForUpdate(requestId);
        RepairRequestStatus fromStatus = repairRequest.getStatus();
        if (activeAssignmentForUpdate(requestId) != null) {
            throw alreadyAssigned();
        }
        Technician technician = availableTechnicianForUpdate(request.technicianId());
        User assignedBy = userRepository.findById(user.id()).orElseThrow(this::userNotFound);
        OffsetDateTime scheduledVisitAt = validateScheduledVisit(request.scheduledVisitAt(), now, false);
        RepairAssignment assignment = new RepairAssignment(repairRequest, technician, scheduledVisitAt, assignedBy, now);
        saveAssignment(assignment);
        applyRequestStatus(repairRequest, assignment, now);
        statusHistoryService.recordTransition(repairRequest, fromStatus, "Technician assigned.", assignedBy, now);
        enqueueAssignmentCreated(repairRequest, assignment);
        publishDomainEvent(new RequestAssignmentCreatedDomainEvent(
                repairRequest.getId(),
                repairRequest.getRequestNumber(),
                technician.getId(),
                assignment.getId(),
                repairRequest.getCustomer().getId()));
        return details(repairRequest);
    }

    @Transactional
    public RepairRequestDetailResponse reassign(Long requestId, ReassignmentRequest request, AuthenticatedUser user) {
        OffsetDateTime now = now();
        RepairRequest repairRequest = assignableRequestForUpdate(requestId);
        RepairRequestStatus fromStatus = repairRequest.getStatus();
        RepairAssignment current = requiredActiveAssignmentForUpdate(requestId);
        if (current.getTechnician().getId().equals(request.technicianId())) {
            throw alreadyAssigned();
        }
        Technician technician = availableTechnicianForUpdate(request.technicianId());
        User assignedBy = userRepository.findById(user.id()).orElseThrow(this::userNotFound);
        current.reassign(validateReason(request.reason()), now);
        repairAssignmentRepository.saveAndFlush(current);
        OffsetDateTime scheduledVisitAt = validateScheduledVisit(request.scheduledVisitAt(), now, false);
        RepairAssignment next = new RepairAssignment(repairRequest, technician, scheduledVisitAt, assignedBy, now);
        saveAssignment(next);
        applyRequestStatus(repairRequest, next, now);
        statusHistoryService.recordTransition(repairRequest, fromStatus, request.reason(), assignedBy, now);
        if (chatService != null) {
            chatService.handleTechnicianReassigned(repairRequest.getId(), current.getTechnician().getId(), technician.getId());
        }
        String eventPart = "assignment:%d:reassigned-to:%d".formatted(current.getId(), next.getId());
        notificationOutboxService.enqueue(notificationEventFactory.technician(
                NotificationType.TECHNICIAN_UNASSIGNED,
                repairRequest,
                current,
                eventPart));
        notificationOutboxService.enqueue(notificationEventFactory.technician(
                NotificationType.TECHNICIAN_ASSIGNED,
                repairRequest,
                next,
                eventPart));
        publishDomainEvent(new RequestReassignedDomainEvent(
                repairRequest.getId(),
                repairRequest.getRequestNumber(),
                current.getTechnician().getId(),
                technician.getId(),
                next.getId(),
                repairRequest.getCustomer().getId()));
        return details(repairRequest);
    }

    @Transactional
    public RepairRequestDetailResponse unassign(Long requestId, UnassignmentRequest request, AuthenticatedUser user) {
        OffsetDateTime now = now();
        RepairRequest repairRequest = assignableRequestForUpdate(requestId);
        RepairRequestStatus fromStatus = repairRequest.getStatus();
        RepairAssignment assignment = requiredActiveAssignmentForUpdate(requestId);
        String reason = validateReason(request.reason());
        User changedBy = userRepository.findById(user.id()).orElseThrow(this::userNotFound);
        assignment.unassign(reason, now);
        repairRequest.returnToNew(now);
        statusHistoryService.recordTransition(repairRequest, fromStatus, reason, changedBy, now);
        if (chatService != null) {
            chatService.handleTechnicianUnassigned(repairRequest.getId(), assignment.getTechnician().getId());
        }
        String eventPart = "assignment:%d:unassigned".formatted(assignment.getId());
        notificationOutboxService.enqueue(notificationEventFactory.technician(
                NotificationType.TECHNICIAN_UNASSIGNED,
                repairRequest,
                assignment,
                eventPart));
        notificationOutboxService.enqueue(notificationEventFactory.customer(
                NotificationType.TECHNICIAN_UNASSIGNED,
                repairRequest,
                eventPart));
        publishDomainEvent(new RequestUnassignedDomainEvent(
                repairRequest.getId(),
                repairRequest.getRequestNumber(),
                repairRequest.getCustomer().getId(),
                assignment.getTechnician().getId(),
                assignment.getId()));
        return details(repairRequest);
    }

    @Transactional
    public RepairRequestDetailResponse schedule(Long requestId, ScheduleRequest request, AuthenticatedUser user) {
        if (request == null) {
            throw invalidScheduledVisit("Schedule request body is required.");
        }
        OffsetDateTime now = now();
        RepairRequest repairRequest = assignableRequestForUpdate(requestId);
        RepairRequestStatus fromStatus = repairRequest.getStatus();
        RepairAssignment assignment = requiredActiveAssignmentForUpdate(requestId);
        User changedBy = userRepository.findById(user.id()).orElseThrow(this::userNotFound);
        OffsetDateTime previousScheduledVisitAt = assignment.getScheduledVisitAt();
        OffsetDateTime scheduledVisitAt = scheduleValue(request, now);
        if (Objects.equals(previousScheduledVisitAt, scheduledVisitAt)) {
            return details(repairRequest);
        }
        assignment.updateSchedule(scheduledVisitAt, now);
        applyRequestStatus(repairRequest, assignment, now);
        statusHistoryService.recordTransition(repairRequest, fromStatus, "Schedule changed.", changedBy, now);
        enqueueScheduleChanged(repairRequest, assignment, previousScheduledVisitAt, scheduledVisitAt);
        String scheduleAction = scheduledVisitAt == null
                ? "CLEARED"
                : (previousScheduledVisitAt == null ? "SCHEDULED" : "RESCHEDULED");
        publishDomainEvent(new RequestScheduleChangedDomainEvent(
                repairRequest.getId(),
                repairRequest.getRequestNumber(),
                assignment.getId(),
                assignment.getTechnician().getId(),
                repairRequest.getCustomer().getId(),
                scheduledVisitAt,
                scheduledVisitAt != null ? scheduledVisitAt.plusHours(2) : null,
                scheduleAction));
        return details(repairRequest);
    }

    @Transactional
    public RepairRequestDetailResponse accept(Long requestId, AuthenticatedUser user) {
        OffsetDateTime now = now();
        RepairRequest repairRequest = assignableRequestForUpdate(requestId);
        RepairAssignment assignment = requiredActiveAssignmentForUpdate(requestId);
        if (assignment.getStatus() == AssignmentStatus.ACCEPTED) {
            throw new BusinessRuleException(
                    "ASSIGNMENT_ALREADY_ACCEPTED",
                    "Assignment has already been accepted.",
                    409);
        }
        if (!assignment.isPending()) {
            throw assignmentNotPending();
        }
        assignment.accept(now);
        applyRequestStatus(repairRequest, assignment, now);
        enqueueAssignmentAccepted(repairRequest, assignment);
        publishDomainEvent(new RequestAssignmentAcceptedDomainEvent(
                repairRequest.getId(),
                repairRequest.getRequestNumber(),
                repairRequest.getCustomer().getId(),
                assignment.getTechnician().getId(),
                assignment.getId()));
        return details(repairRequest);
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public RepairRequestDetailResponse acceptByTechnician(Long requestId, Long technicianId) {
        OffsetDateTime now = now();
        RepairRequest repairRequest = assignableRequestForUpdate(requestId);
        RepairAssignment assignment = requiredActiveAssignmentForUpdate(requestId);
        activeTechnicianForUpdate(technicianId);
        requireTechnicianAssignment(assignment, technicianId);
        if (assignment.getStatus() == AssignmentStatus.ACCEPTED) {
            throw new BusinessRuleException(
                    "ASSIGNMENT_ALREADY_ACCEPTED",
                    "Assignment has already been accepted.",
                    409);
        }
        if (!assignment.isPending()) {
            throw assignmentNotPending();
        }
        assignment.accept(now);
        applyRequestStatus(repairRequest, assignment, now);
        enqueueAssignmentAccepted(repairRequest, assignment);
        publishDomainEvent(new RequestAssignmentAcceptedDomainEvent(
                repairRequest.getId(),
                repairRequest.getRequestNumber(),
                repairRequest.getCustomer().getId(),
                assignment.getTechnician().getId(),
                assignment.getId()));
        return details(repairRequest);
    }

    @Transactional
    public RepairRequestDetailResponse reject(
            Long requestId,
            AssignmentRejectionRequest request,
            AuthenticatedUser user) {
        OffsetDateTime now = now();
        RepairRequest repairRequest = assignableRequestForUpdate(requestId);
        RepairRequestStatus fromStatus = repairRequest.getStatus();
        RepairAssignment assignment = requiredActiveAssignmentForUpdate(requestId);
        if (assignment.getStatus() == AssignmentStatus.REJECTED) {
            throw new BusinessRuleException(
                    "ASSIGNMENT_ALREADY_REJECTED",
                    "Assignment has already been rejected.",
                    409);
        }
        if (!assignment.isPending()) {
            throw assignmentNotPending();
        }
        String reason = validateReason(request.reason());
        User changedBy = userRepository.findById(user.id()).orElseThrow(this::userNotFound);
        assignment.reject(reason, now);
        repairRequest.returnToNew(now);
        statusHistoryService.recordTransition(repairRequest, fromStatus, reason, changedBy, now);
        if (chatService != null) {
            chatService.handleTechnicianUnassigned(repairRequest.getId(), assignment.getTechnician().getId());
        }
        notifyStaffAssignmentRejected(repairRequest, assignment, reason);
        publishDomainEvent(new RequestAssignmentRejectedDomainEvent(
                repairRequest.getId(),
                repairRequest.getRequestNumber(),
                repairRequest.getCustomer().getId(),
                assignment.getTechnician().getId(),
                assignment.getId(),
                reason));
        return details(repairRequest);
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public RepairRequestDetailResponse rejectByTechnician(
            Long requestId,
            AssignmentRejectionRequest request,
            Long technicianId) {
        OffsetDateTime now = now();
        RepairRequest repairRequest = assignableRequestForUpdate(requestId);
        RepairRequestStatus fromStatus = repairRequest.getStatus();
        RepairAssignment assignment = requiredActiveAssignmentForUpdate(requestId);
        Technician changedBy = activeTechnicianForUpdate(technicianId);
        requireTechnicianAssignment(assignment, technicianId);
        if (assignment.getStatus() == AssignmentStatus.REJECTED) {
            throw new BusinessRuleException(
                    "ASSIGNMENT_ALREADY_REJECTED",
                    "Assignment has already been rejected.",
                    409);
        }
        if (!assignment.isPending()) {
            throw assignmentNotPending();
        }
        String reason = validateReason(request.reason());
        assignment.reject(reason, now);
        repairRequest.returnToNew(now);
        statusHistoryService.recordTransition(repairRequest, fromStatus, reason, changedBy, now);
        if (chatService != null) {
            chatService.handleTechnicianUnassigned(repairRequest.getId(), assignment.getTechnician().getId());
        }
        notifyStaffAssignmentRejected(repairRequest, assignment, reason);
        publishDomainEvent(new RequestAssignmentRejectedDomainEvent(
                repairRequest.getId(),
                repairRequest.getRequestNumber(),
                repairRequest.getCustomer().getId(),
                assignment.getTechnician().getId(),
                assignment.getId(),
                reason));
        return details(repairRequest);
    }

    private void publishDomainEvent(Object event) {
        if (applicationEventPublisher != null) {
            applicationEventPublisher.publishEvent(event);
        }
    }

    @Transactional(readOnly = true)
    public List<AssignmentDetailResponse> history(Long requestId) {
        if (!repairRequestRepository.existsById(requestId)) {
            throw requestNotFound();
        }
        return repairAssignmentRepository.findByRepairRequestIdOrderByCreatedAtDesc(requestId)
                .stream()
                .map(AssignmentMapper::details)
                .toList();
    }

    @Transactional(readOnly = true)
    public TechnicianWorkloadResponse workload(Long technicianId) {
        Technician technician = technicianRepository.findById(technicianId).orElseThrow(this::technicianNotFound);
        long pending = repairAssignmentRepository.countByTechnicianIdAndStatus(technicianId, AssignmentStatus.PENDING);
        long accepted = repairAssignmentRepository.countByTechnicianIdAndStatus(technicianId, AssignmentStatus.ACCEPTED);
        long total = pending + accepted;
        long remaining = Math.max(0, technician.getMaximumConcurrentRequests() - total);
        return new TechnicianWorkloadResponse(
                technician.getId(),
                technician.isActive(),
                technician.getMaximumConcurrentRequests(),
                pending,
                accepted,
                total,
                remaining,
                technician.isActive() && remaining > 0);
    }

    private RepairRequestDetailResponse details(RepairRequest repairRequest) {
        RepairAssignment current = repairAssignmentRepository
                .findActiveByRequestId(repairRequest.getId(), RepairAssignmentRepository.ACTIVE_STATUSES)
                .orElse(null);
        Language lang = effectiveLanguageResolver.resolveEffectiveLanguage();
        return RepairRequestMapper.details(repairRequest, current, null, lang, localizedValueResolver);
    }

    private RepairRequest assignableRequestForUpdate(Long requestId) {
        RepairRequest repairRequest = repairRequestRepository.findByIdForUpdate(requestId).orElseThrow(this::requestNotFound);
        if (repairRequest.getStatus() != RepairRequestStatus.NEW
                && repairRequest.getStatus() != RepairRequestStatus.ASSIGNED
                && repairRequest.getStatus() != RepairRequestStatus.SCHEDULED) {
            throw new BusinessRuleException(
                    "REPAIR_REQUEST_NOT_ASSIGNABLE",
                    "Repair request cannot be assigned in its current status.",
                    409);
        }
        return repairRequest;
    }

    private RepairAssignment activeAssignmentForUpdate(Long requestId) {
        return repairAssignmentRepository
                .findActiveByRequestIdForUpdate(requestId, RepairAssignmentRepository.ACTIVE_STATUSES)
                .orElse(null);
    }

    private RepairAssignment requiredActiveAssignmentForUpdate(Long requestId) {
        RepairAssignment assignment = activeAssignmentForUpdate(requestId);
        if (assignment == null) {
            throw new BusinessRuleException(
                    "ACTIVE_ASSIGNMENT_NOT_FOUND",
                    "Active assignment was not found for this repair request.",
                    404);
        }
        return assignment;
    }

    private Technician availableTechnicianForUpdate(Long technicianId) {
        Technician technician = technicianRepository.findByIdForUpdate(technicianId).orElseThrow(this::technicianNotFound);
        if (!technician.isActive()) {
            throw new BusinessRuleException(
                    "TECHNICIAN_INACTIVE",
                    "Inactive technician cannot be assigned to a repair request.",
                    409);
        }
        long activeAssignments = repairAssignmentRepository.countByTechnicianIdAndStatusIn(
                technicianId,
                RepairAssignmentRepository.ACTIVE_STATUSES);
        if (activeAssignments >= technician.getMaximumConcurrentRequests()) {
            throw new BusinessRuleException(
                    "TECHNICIAN_CAPACITY_EXCEEDED",
                    "Technician has no remaining assignment capacity.",
                    409);
        }
        return technician;
    }

    private Technician activeTechnicianForUpdate(Long technicianId) {
        Technician technician = technicianRepository.findByIdForUpdate(technicianId).orElseThrow(this::technicianNotFound);
        if (!technician.isActive()) {
            throw new BusinessRuleException(
                    "TECHNICIAN_INACTIVE",
                    "Inactive technician cannot perform this action.",
                    409);
        }
        return technician;
    }

    private OffsetDateTime scheduleValue(ScheduleRequest request, OffsetDateTime now) {
        boolean clear = Boolean.TRUE.equals(request.clearSchedule());
        if (clear && request.scheduledVisitAt() != null) {
            throw invalidScheduledVisit("Provide either clearSchedule=true or scheduledVisitAt, not both.");
        }
        if (clear) {
            return null;
        }
        return validateScheduledVisit(request.scheduledVisitAt(), now, true);
    }

    private OffsetDateTime validateScheduledVisit(OffsetDateTime value, OffsetDateTime now, boolean required) {
        if (value == null) {
            if (required) {
                throw invalidScheduledVisit("scheduledVisitAt is required unless clearSchedule=true.");
            }
            return null;
        }
        OffsetDateTime utc = value.withOffsetSameInstant(ZoneOffset.UTC);
        if (!utc.isAfter(now)) {
            throw invalidScheduledVisit("Scheduled visit time must be in the future.");
        }
        return utc;
    }

    private String validateReason(String value) {
        String reason = blankToNull(value);
        if (reason == null || reason.length() > MAX_REASON_LENGTH) {
            throw new BusinessRuleException(
                    "VALIDATION_FAILED",
                    "Reason must be between 1 and 500 characters.",
                    400);
        }
        return reason;
    }

    private void applyRequestStatus(RepairRequest request, RepairAssignment assignment, OffsetDateTime now) {
        if (assignment.getScheduledVisitAt() == null) {
            request.markAssigned(now);
        } else {
            request.markScheduled(now);
        }
    }

    private void saveAssignment(RepairAssignment assignment) {
        try {
            repairAssignmentRepository.saveAndFlush(assignment);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessRuleException(
                    "ASSIGNMENT_CONFLICT",
                    "Assignment state changed concurrently. Reload and try again.",
                    409);
        }
    }

    private void enqueueAssignmentCreated(RepairRequest request, RepairAssignment assignment) {
        String eventPart = "assignment:%d:created".formatted(assignment.getId());
        notificationOutboxService.enqueue(notificationEventFactory.technician(
                NotificationType.TECHNICIAN_ASSIGNED,
                request,
                assignment,
                eventPart));
    }

    private void enqueueAssignmentAccepted(RepairRequest request, RepairAssignment assignment) {
        String eventPart = "assignment:%d:accepted".formatted(assignment.getId());
        notificationOutboxService.enqueue(notificationEventFactory.customer(
                NotificationType.TECHNICIAN_ASSIGNED,
                request,
                assignment,
                eventPart));
    }

    private void notifyStaffAssignmentRejected(
            RepairRequest request,
            RepairAssignment assignment,
            String reason) {
        List<User> staffUsers = userRepository.findActiveStaff();
        String eventPart = "assignment:%d:rejected".formatted(assignment.getId());
        for (User staffUser : staffUsers) {
            notificationOutboxService.enqueue(notificationEventFactory.staff(
                    NotificationType.TECHNICIAN_REJECTED,
                    request,
                    assignment,
                    reason,
                    staffUser.getId(),
                    eventPart));
        }
    }

    private void enqueueScheduleChanged(
            RepairRequest request,
            RepairAssignment assignment,
            OffsetDateTime previousScheduledVisitAt,
            OffsetDateTime scheduledVisitAt) {
        NotificationType customerType;
        NotificationType technicianType;
        if (scheduledVisitAt == null) {
            customerType = NotificationType.VISIT_CANCELLED;
            technicianType = NotificationType.VISIT_CANCELLED;
        } else if (previousScheduledVisitAt == null) {
            customerType = NotificationType.VISIT_SCHEDULED;
            technicianType = NotificationType.VISIT_SCHEDULED;
        } else {
            customerType = NotificationType.VISIT_RESCHEDULED;
            technicianType = NotificationType.VISIT_RESCHEDULED;
        }
        String schedulePart = scheduledVisitAt == null ? "cleared" : scheduledVisitAt.toInstant().toString();
        String eventPart = "assignment:%d:schedule:%s".formatted(assignment.getId(), schedulePart);
        notificationOutboxService.enqueue(notificationEventFactory.customer(
                customerType,
                request,
                assignment,
                eventPart));
        notificationOutboxService.enqueue(notificationEventFactory.technician(
                technicianType,
                request,
                assignment,
                eventPart));
    }

    private void requireTechnicianAssignment(RepairAssignment assignment, Long technicianId) {
        if (!assignment.getTechnician().getId().equals(technicianId)) {
            throw new BusinessRuleException(
                    "TECHNICIAN_ASSIGNMENT_FORBIDDEN",
                    "Repair request belongs to another technician.",
                    403);
        }
    }

    private BusinessRuleException alreadyAssigned() {
        return new BusinessRuleException(
                "REPAIR_REQUEST_ALREADY_ASSIGNED",
                "Repair request already has an active assignment.",
                409);
    }

    private BusinessRuleException assignmentNotPending() {
        return new BusinessRuleException(
                "ASSIGNMENT_NOT_PENDING",
                "Assignment must be pending for this action.",
                409);
    }

    private BusinessRuleException invalidScheduledVisit(String message) {
        return new BusinessRuleException("INVALID_SCHEDULED_VISIT_TIME", message, 400);
    }

    private BusinessRuleException requestNotFound() {
        return new BusinessRuleException("REPAIR_REQUEST_NOT_FOUND", "Repair request was not found.", 404);
    }

    private BusinessRuleException technicianNotFound() {
        return new BusinessRuleException("TECHNICIAN_NOT_FOUND", "Technician was not found.", 404);
    }

    private BusinessRuleException userNotFound() {
        return new BusinessRuleException("USER_NOT_FOUND", "Authenticated user was not found.", 404);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
