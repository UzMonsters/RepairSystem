package com.example.darks.repair_auto.repair.attachment.mobile.application;

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
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MobileAttachmentFacade {

    public static final Set<AttachmentType> CUSTOMER_UPLOADABLE_TYPES = Set.of(
            AttachmentType.CUSTOMER_PROBLEM_PHOTO);

    public static final Set<AttachmentType> TECHNICIAN_UPLOADABLE_TYPES = Set.of(
            AttachmentType.DIAGNOSIS_PHOTO,
            AttachmentType.COMPLETION_PHOTO);

    public static final Set<AttachmentType> CUSTOMER_VISIBLE_TYPES = Set.of(
            AttachmentType.CUSTOMER_PROBLEM_PHOTO,
            AttachmentType.DIAGNOSIS_PHOTO,
            AttachmentType.COMPLETION_PHOTO);

    public static final Set<AttachmentType> TECHNICIAN_VISIBLE_TYPES = Set.of(
            AttachmentType.CUSTOMER_PROBLEM_PHOTO,
            AttachmentType.DIAGNOSIS_PHOTO,
            AttachmentType.COMPLETION_PHOTO);

    private final AttachmentService attachmentService;
    private final RepairResourceAccessPolicy accessPolicy;
    private final RepairAttachmentRepository attachmentRepository;

    public MobileAttachmentFacade(
            AttachmentService attachmentService,
            RepairResourceAccessPolicy accessPolicy,
            RepairAttachmentRepository attachmentRepository) {
        this.attachmentService = attachmentService;
        this.accessPolicy = accessPolicy;
        this.attachmentRepository = attachmentRepository;
    }

    public MobileAttachmentResponse uploadCustomerAttachment(
            AuthenticatedMobileActor actor,
            Long requestId,
            AttachmentType type,
            MultipartFile file) {
        AttachmentType effectiveType = type == null ? AttachmentType.CUSTOMER_PROBLEM_PHOTO : type;
        if (!CUSTOMER_UPLOADABLE_TYPES.contains(effectiveType)) {
            throw new BusinessRuleException(
                    "ATTACHMENT_TYPE_NOT_ALLOWED",
                    "Customer can only upload customer problem photos.",
                    400);
        }
        accessPolicy.requireCurrentCustomerOwnsRequest(actor, requestId);
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException(
                    "ATTACHMENT_EMPTY",
                    "Attachment file is required and must be non-empty.",
                    400);
        }
        try (InputStream inputStream = file.getInputStream()) {
            AttachmentResponse response = attachmentService.uploadFromCustomer(
                    requestId,
                    effectiveType,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    inputStream,
                    actor.actorId());
            return toMobileResponse(response);
        } catch (IOException exception) {
            throw new BusinessRuleException("ATTACHMENT_STORAGE_FAILED", "Attachment upload failed.", 503);
        }
    }

    @Transactional(readOnly = true)
    public List<MobileAttachmentResponse> listCustomerAttachments(AuthenticatedMobileActor actor, Long requestId) {
        accessPolicy.requireCurrentCustomerCanReadRequest(actor, requestId);
        List<RepairAttachment> attachments = attachmentRepository
                .findByRepairRequestIdAndStatusAndAttachmentTypeInOrderByUploadedAtAsc(
                        requestId,
                        AttachmentStatus.AVAILABLE,
                        CUSTOMER_VISIBLE_TYPES);
        return attachments.stream().map(this::toMobileResponse).toList();
    }

    public MobileAttachmentResponse uploadTechnicianAttachment(
            AuthenticatedMobileActor actor,
            Long requestId,
            AttachmentType type,
            MultipartFile file) {
        if (type == null || !TECHNICIAN_UPLOADABLE_TYPES.contains(type)) {
            throw new BusinessRuleException(
                    "ATTACHMENT_TYPE_NOT_ALLOWED",
                    "Technician can only upload diagnosis or completion photos.",
                    400);
        }
        accessPolicy.requireCurrentTechnicianCurrentAssignment(actor, requestId);
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException(
                    "ATTACHMENT_EMPTY",
                    "Attachment file is required and must be non-empty.",
                    400);
        }
        try (InputStream inputStream = file.getInputStream()) {
            AttachmentResponse response = attachmentService.uploadFromTechnician(
                    requestId,
                    type,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    inputStream,
                    actor.actorId());
            return toMobileResponse(response);
        } catch (IOException exception) {
            throw new BusinessRuleException("ATTACHMENT_STORAGE_FAILED", "Attachment upload failed.", 503);
        }
    }

    @Transactional(readOnly = true)
    public List<MobileAttachmentResponse> listTechnicianAttachments(AuthenticatedMobileActor actor, Long requestId) {
        accessPolicy.requireCurrentTechnicianCanReadRequest(actor, requestId);
        List<RepairAttachment> attachments = attachmentRepository
                .findByRepairRequestIdAndStatusAndAttachmentTypeInOrderByUploadedAtAsc(
                        requestId,
                        AttachmentStatus.AVAILABLE,
                        TECHNICIAN_VISIBLE_TYPES);
        return attachments.stream().map(this::toMobileResponse).toList();
    }

    @Transactional(readOnly = true)
    public MobileAttachmentDownloadUrlResponse getDownloadUrl(AuthenticatedMobileActor actor, Long attachmentId) {
        if (actor == null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (actor.isCustomer()) {
            RepairAttachment attachment = accessPolicy.requireCurrentCustomerCanAccessAttachment(actor, attachmentId);
            if (!CUSTOMER_VISIBLE_TYPES.contains(attachment.getAttachmentType())) {
                throw new BusinessException(ErrorCode.ATTACHMENT_NOT_FOUND);
            }
        } else if (actor.isTechnician()) {
            RepairAttachment attachment = accessPolicy.requireCurrentTechnicianCanAccessAttachment(actor, attachmentId);
            if (!TECHNICIAN_VISIBLE_TYPES.contains(attachment.getAttachmentType())) {
                throw new BusinessException(ErrorCode.ATTACHMENT_NOT_FOUND);
            }
        } else {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        DownloadUrlResponse downloadUrl = attachmentService.downloadUrl(attachmentId);
        return new MobileAttachmentDownloadUrlResponse(
                attachmentId,
                downloadUrl.url(),
                downloadUrl.expiresAt());
    }

    private MobileAttachmentResponse toMobileResponse(AttachmentResponse response) {
        return new MobileAttachmentResponse(
                response.id(),
                response.repairRequestId(),
                response.type(),
                response.originalFileName(),
                response.contentType(),
                response.sizeBytes(),
                response.status(),
                response.uploadedAt());
    }

    private MobileAttachmentResponse toMobileResponse(RepairAttachment attachment) {
        return new MobileAttachmentResponse(
                attachment.getId(),
                attachment.getRepairRequest().getId(),
                attachment.getAttachmentType(),
                attachment.getOriginalFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getStatus(),
                attachment.getUploadedAt());
    }
}
