package com.example.darks.repair_auto.repair.attachment.application;

import com.example.darks.repair_auto.repair.attachment.domain.AttachmentStatus;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import org.springframework.stereotype.Service;

@Service
public class AttachmentRequirementService {

    private final RepairAttachmentRepository attachmentRepository;

    public AttachmentRequirementService(RepairAttachmentRepository attachmentRepository) {
        this.attachmentRepository = attachmentRepository;
    }

    public void requireAvailableCompletionPhoto(Long requestId) {
        attachmentRepository.lockByRequestIdAndAttachmentTypeAndStatusIn(
                requestId,
                AttachmentType.COMPLETION_PHOTO,
                RepairAttachmentRepository.BUSINESS_VISIBLE_STATUSES);
        long count = attachmentRepository.countByRepairRequestIdAndAttachmentTypeAndStatus(
                requestId,
                AttachmentType.COMPLETION_PHOTO,
                AttachmentStatus.AVAILABLE);
        if (count < 1) {
            throw new BusinessRuleException(
                    "COMPLETION_PHOTO_REQUIRED",
                    "At least one available completion photo is required before completion.",
                    409);
        }
    }
}
