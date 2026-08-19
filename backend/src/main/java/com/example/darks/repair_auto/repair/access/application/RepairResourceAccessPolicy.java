package com.example.darks.repair_auto.repair.access.application;

import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.repair.assignment.domain.AssignmentStatus;
import com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentStatus;
import com.example.darks.repair_auto.repair.attachment.domain.RepairAttachment;
import com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RepairResourceAccessPolicy {

    public static final Set<AssignmentStatus> ACTIVE_ASSIGNMENT_STATUSES = Set.of(
            AssignmentStatus.PENDING,
            AssignmentStatus.ACCEPTED);

    public static final Set<AssignmentStatus> READABLE_TECHNICIAN_ASSIGNMENT_STATUSES = Set.of(
            AssignmentStatus.PENDING,
            AssignmentStatus.ACCEPTED,
            AssignmentStatus.COMPLETED,
            AssignmentStatus.CANCELLED);

    private final RepairRequestRepository repairRequestRepository;
    private final RepairAssignmentRepository repairAssignmentRepository;
    private final RepairAttachmentRepository repairAttachmentRepository;

    public RepairResourceAccessPolicy(
            RepairRequestRepository repairRequestRepository,
            RepairAssignmentRepository repairAssignmentRepository,
            RepairAttachmentRepository repairAttachmentRepository) {
        this.repairRequestRepository = repairRequestRepository;
        this.repairAssignmentRepository = repairAssignmentRepository;
        this.repairAttachmentRepository = repairAttachmentRepository;
    }

    @Transactional(readOnly = true)
    public RepairRequest requireCustomerOwnsRequest(Long customerId, Long requestId) {
        if (customerId == null || requestId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return repairRequestRepository.findByIdAndCustomerId(requestId, customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPAIR_REQUEST_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public RepairRequest requireCustomerCanReadRequest(Long customerId, Long requestId) {
        return requireCustomerOwnsRequest(customerId, requestId);
    }

    @Transactional(readOnly = true)
    public RepairAttachment requireCustomerCanAccessAttachment(Long customerId, Long attachmentId) {
        if (customerId == null || attachmentId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        RepairAttachment attachment = repairAttachmentRepository.findByIdAndStatus(attachmentId, AttachmentStatus.AVAILABLE)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTACHMENT_NOT_FOUND));

        Long requestId = attachment.getRepairRequest().getId();
        requireCustomerOwnsRequest(customerId, requestId);
        return attachment;
    }

    @Transactional(readOnly = true)
    public RepairAssignment requireTechnicianCurrentAssignment(Long technicianId, Long requestId) {
        if (technicianId == null || requestId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        RepairAssignment assignment = repairAssignmentRepository.findActiveByRequestId(requestId, ACTIVE_ASSIGNMENT_STATUSES)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACTIVE_ASSIGNMENT_NOT_FOUND));

        if (!assignment.getTechnician().getId().equals(technicianId)) {
            throw new BusinessException(ErrorCode.ACTIVE_ASSIGNMENT_NOT_FOUND);
        }
        return assignment;
    }

    @Transactional(readOnly = true)
    public RepairRequest requireTechnicianCanReadRequest(Long technicianId, Long requestId) {
        if (technicianId == null || requestId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        RepairRequest request = repairRequestRepository.findWithRelationsById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPAIR_REQUEST_NOT_FOUND));

        List<RepairAssignment> readableAssignments = repairAssignmentRepository
                .findByRepairRequestIdAndTechnicianIdAndStatusInOrderByCreatedAtDesc(
                        requestId,
                        technicianId,
                        READABLE_TECHNICIAN_ASSIGNMENT_STATUSES);

        if (readableAssignments.isEmpty()) {
            throw new BusinessException(ErrorCode.REPAIR_REQUEST_NOT_FOUND);
        }

        return request;
    }

    @Transactional(readOnly = true)
    public RepairAttachment requireTechnicianCanAccessAttachment(Long technicianId, Long attachmentId) {
        if (technicianId == null || attachmentId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        RepairAttachment attachment = repairAttachmentRepository.findByIdAndStatus(attachmentId, AttachmentStatus.AVAILABLE)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTACHMENT_NOT_FOUND));

        Long requestId = attachment.getRepairRequest().getId();
        requireTechnicianCanReadRequest(technicianId, requestId);
        return attachment;
    }

    @Transactional(readOnly = true)
    public RepairRequest requireCurrentCustomerOwnsRequest(AuthenticatedMobileActor actor, Long requestId) {
        requireCustomerActor(actor);
        return requireCustomerOwnsRequest(actor.actorId(), requestId);
    }

    @Transactional(readOnly = true)
    public RepairRequest requireCurrentCustomerCanReadRequest(AuthenticatedMobileActor actor, Long requestId) {
        requireCustomerActor(actor);
        return requireCustomerCanReadRequest(actor.actorId(), requestId);
    }

    @Transactional(readOnly = true)
    public RepairAttachment requireCurrentCustomerCanAccessAttachment(AuthenticatedMobileActor actor, Long attachmentId) {
        requireCustomerActor(actor);
        return requireCustomerCanAccessAttachment(actor.actorId(), attachmentId);
    }

    @Transactional(readOnly = true)
    public RepairRequest requireCurrentTechnicianCanReadRequest(AuthenticatedMobileActor actor, Long requestId) {
        requireTechnicianActor(actor);
        return requireTechnicianCanReadRequest(actor.actorId(), requestId);
    }

    @Transactional(readOnly = true)
    public RepairAssignment requireCurrentTechnicianCurrentAssignment(AuthenticatedMobileActor actor, Long requestId) {
        requireTechnicianActor(actor);
        return requireTechnicianCurrentAssignment(actor.actorId(), requestId);
    }

    @Transactional(readOnly = true)
    public RepairAttachment requireCurrentTechnicianCanAccessAttachment(AuthenticatedMobileActor actor, Long attachmentId) {
        requireTechnicianActor(actor);
        return requireTechnicianCanAccessAttachment(actor.actorId(), attachmentId);
    }

    private void requireCustomerActor(AuthenticatedMobileActor actor) {
        if (actor == null || !actor.isCustomer()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (!actor.active()) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }
    }

    private void requireTechnicianActor(AuthenticatedMobileActor actor) {
        if (actor == null || !actor.isTechnician()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (!actor.active()) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }
    }
}
