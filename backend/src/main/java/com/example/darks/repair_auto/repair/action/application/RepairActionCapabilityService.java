package com.example.darks.repair_auto.repair.action.application;

import com.example.darks.repair_auto.repair.action.domain.RepairAvailableAction;
import com.example.darks.repair_auto.repair.assignment.domain.AssignmentStatus;
import com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentStatus;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository;
import com.example.darks.repair_auto.repair.execution.domain.RepairExecution;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.review.infrastructure.RepairReviewRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepairActionCapabilityService {

    private final RepairAttachmentRepository attachmentRepository;
    private final RepairReviewRepository reviewRepository;

    public RepairActionCapabilityService(
            RepairAttachmentRepository attachmentRepository,
            RepairReviewRepository reviewRepository) {
        this.attachmentRepository = attachmentRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public List<RepairAvailableAction> resolveTechnicianActions(
            RepairRequest request,
            RepairAssignment assignment,
            RepairExecution execution) {
        if (request == null || assignment == null) {
            return List.of();
        }

        if (request.getStatus() == RepairRequestStatus.COMPLETED
                || request.getStatus() == RepairRequestStatus.CANCELLED
                || assignment.getStatus() == AssignmentStatus.COMPLETED
                || assignment.getStatus() == AssignmentStatus.CANCELLED
                || assignment.getStatus() == AssignmentStatus.REJECTED
                || assignment.getStatus() == AssignmentStatus.UNASSIGNED
                || assignment.getStatus() == AssignmentStatus.REASSIGNED) {
            return List.of();
        }

        List<RepairAvailableAction> actions = new ArrayList<>();

        if (assignment.getStatus() == AssignmentStatus.PENDING) {
            actions.add(RepairAvailableAction.ACCEPT_ASSIGNMENT);
            actions.add(RepairAvailableAction.REJECT_ASSIGNMENT);
            return Collections.unmodifiableList(actions);
        }

        if (assignment.getStatus() == AssignmentStatus.ACCEPTED) {
            RepairRequestStatus requestStatus = request.getStatus();

            if ((requestStatus == RepairRequestStatus.ASSIGNED || requestStatus == RepairRequestStatus.SCHEDULED)
                    && (execution == null || !execution.hasStarted())) {
                actions.add(RepairAvailableAction.START_REPAIR);
            } else if (requestStatus == RepairRequestStatus.IN_PROGRESS) {
                actions.add(RepairAvailableAction.UPDATE_DIAGNOSIS);
                actions.add(RepairAvailableAction.WAIT_FOR_PARTS);
                actions.add(RepairAvailableAction.UPLOAD_DIAGNOSIS_PHOTO);
                actions.add(RepairAvailableAction.UPLOAD_COMPLETION_PHOTO);

                if (canCompleteRepair(request, execution)) {
                    actions.add(RepairAvailableAction.COMPLETE_REPAIR);
                }
            } else if (requestStatus == RepairRequestStatus.WAITING_FOR_PARTS) {
                actions.add(RepairAvailableAction.UPDATE_DIAGNOSIS);
                actions.add(RepairAvailableAction.RESUME_REPAIR);
                actions.add(RepairAvailableAction.UPLOAD_DIAGNOSIS_PHOTO);
            }
        }

        return Collections.unmodifiableList(actions);
    }

    @Transactional(readOnly = true)
    public List<RepairAvailableAction> resolveCustomerActions(RepairRequest request) {
        if (request == null) {
            return List.of();
        }
        if (request.getStatus() == RepairRequestStatus.COMPLETED) {
            boolean hasReview = reviewRepository.existsByRepairRequestId(request.getId());
            if (!hasReview) {
                return List.of(RepairAvailableAction.SUBMIT_REVIEW);
            }
            return List.of();
        }
        if (request.getStatus() == RepairRequestStatus.CANCELLED) {
            return List.of();
        }
        return List.of(RepairAvailableAction.UPLOAD_PROBLEM_PHOTO);
    }

    private boolean canCompleteRepair(RepairRequest request, RepairExecution execution) {
        if (execution == null || !execution.hasDiagnosis()) {
            return false;
        }
        long photoCount = attachmentRepository.countByRepairRequestIdAndAttachmentTypeAndStatus(
                request.getId(),
                AttachmentType.COMPLETION_PHOTO,
                AttachmentStatus.AVAILABLE);
        return photoCount > 0;
    }
}
