package com.example.darks.repair_auto.repair.attachment.application;

import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.repair.attachment.api.dto.AttachmentDeleteRequest;
import com.example.darks.repair_auto.repair.attachment.api.dto.AttachmentMapper;
import com.example.darks.repair_auto.repair.attachment.api.dto.AttachmentResponse;
import com.example.darks.repair_auto.repair.attachment.api.dto.DownloadUrlResponse;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentStatus;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.repair.attachment.domain.RepairAttachment;
import com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.ObjectStorageService;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StorageException;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StorageProperties;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StorageUpload;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.error.ResourceNotFoundException;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttachmentService.class);
    private static final int STREAM_MARK_LIMIT = 64;
    private static final int FAILURE_REASON_LIMIT = 120;

    private final RepairAttachmentRepository attachmentRepository;
    private final RepairRequestRepository repairRequestRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final TechnicianRepository technicianRepository;
    private final ObjectStorageService objectStorageService;
    private final StorageProperties storageProperties;
    private final AttachmentValidator validator;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    @Autowired
    public AttachmentService(
            RepairAttachmentRepository attachmentRepository,
            RepairRequestRepository repairRequestRepository,
            UserRepository userRepository,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            ObjectStorageService objectStorageService,
            StorageProperties storageProperties,
            AttachmentValidator validator,
            TransactionTemplate transactionTemplate) {
        this(
                attachmentRepository,
                repairRequestRepository,
                userRepository,
                customerRepository,
                technicianRepository,
                objectStorageService,
                storageProperties,
                validator,
                transactionTemplate,
                Clock.systemUTC());
    }

    AttachmentService(
            RepairAttachmentRepository attachmentRepository,
            RepairRequestRepository repairRequestRepository,
            UserRepository userRepository,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            ObjectStorageService objectStorageService,
            StorageProperties storageProperties,
            AttachmentValidator validator,
            TransactionTemplate transactionTemplate,
            Clock clock) {
        this.attachmentRepository = attachmentRepository;
        this.repairRequestRepository = repairRequestRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.technicianRepository = technicianRepository;
        this.objectStorageService = objectStorageService;
        this.storageProperties = storageProperties;
        this.validator = validator;
        this.transactionTemplate = transactionTemplate;
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
    }

    public AttachmentResponse upload(Long requestId, AttachmentType type, MultipartFile file, AuthenticatedUser user) {
        if (type == null) {
            throw new BusinessRuleException("ATTACHMENT_TYPE_NOT_ALLOWED", "Attachment type is required.", 400);
        }
        String originalFileName = validator.validateOriginalFileName(file);
        try {
            return uploadStream(
                    requestId,
                    type,
                    originalFileName,
                    file.getContentType(),
                    file.getSize(),
                    file.getInputStream(),
                    UploadOwner.staff(user));
        } catch (IOException exception) {
            throw new BusinessRuleException("ATTACHMENT_STORAGE_FAILED", "Attachment upload failed.", 503);
        }
    }

    public AttachmentResponse uploadFromCustomer(
            Long requestId,
            AttachmentType type,
            String originalFileName,
            String declaredContentType,
            long sizeBytes,
            InputStream inputStream,
            Long customerId) {
        if (type == null) {
            throw new BusinessRuleException("ATTACHMENT_TYPE_NOT_ALLOWED", "Attachment type is required.", 400);
        }
        String safeFileName = validator.validateOriginalFileName(originalFileName, sizeBytes);
        return uploadStream(
                requestId,
                type,
                safeFileName,
                declaredContentType,
                sizeBytes,
                inputStream,
                UploadOwner.customer(customerId));
    }

    public AttachmentResponse uploadFromTechnician(
            Long requestId,
            AttachmentType type,
            String originalFileName,
            String declaredContentType,
            long sizeBytes,
            InputStream inputStream,
            Long technicianId) {
        if (type == null) {
            throw new BusinessRuleException("ATTACHMENT_TYPE_NOT_ALLOWED", "Attachment type is required.", 400);
        }
        String safeFileName = validator.validateOriginalFileName(originalFileName, sizeBytes);
        return uploadStream(
                requestId,
                type,
                safeFileName,
                declaredContentType,
                sizeBytes,
                inputStream,
                UploadOwner.technician(technicianId));
    }

    private AttachmentResponse uploadStream(
            Long requestId,
            AttachmentType type,
            String originalFileName,
            String declaredContentType,
            long declaredSize,
            InputStream inputStream,
            UploadOwner owner) {
        RepairAttachment attachment = reserve(requestId, type, originalFileName, owner);
        try (BufferedInputStream input = new BufferedInputStream(inputStream)) {
            input.mark(STREAM_MARK_LIMIT);
            DetectedFile detected = validator.detectAndValidate(type, input);
            validateDeclaredContentType(declaredContentType, detected.contentType());
            input.reset();
            HashingInputStream hashingInput = new HashingInputStream(input, sha256());
            objectStorageService.upload(new StorageUpload(
                    attachment.getStorageKey(),
                    detected.contentType(),
                    declaredSize,
                    hashingInput));
            return finalizeUpload(
                    attachment.getId(),
                    detected.contentType(),
                    hashingInput.sizeBytes(),
                    hashingInput.checksum());
        } catch (BusinessRuleException exception) {
            failUpload(attachment.getId(), "VALIDATION_FAILED");
            bestEffortDelete(attachment.getStorageKey());
            throw exception;
        } catch (StorageException exception) {
            LOGGER.warn(
                    "Attachment object storage upload failed attachmentId={} requestId={} type={} storageKey={} errorType={} message={} rootCauseType={} rootCauseMessage={}",
                    attachment.getId(),
                    requestId,
                    type,
                    attachment.getStorageKey(),
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    rootCause(exception).getClass().getSimpleName(),
                    rootCause(exception).getMessage());
            failUpload(attachment.getId(), "STORAGE_FAILED");
            bestEffortDelete(attachment.getStorageKey());
            throw new BusinessRuleException(
                    "ATTACHMENT_STORAGE_FAILED",
                    "Attachment storage operation failed.",
                    503);
        } catch (IOException exception) {
            failUpload(attachment.getId(), "READ_FAILED");
            bestEffortDelete(attachment.getStorageKey());
            throw new BusinessRuleException("ATTACHMENT_STORAGE_FAILED", "Attachment upload failed.", 503);
        } catch (RuntimeException exception) {
            failUpload(attachment.getId(), "FINALIZATION_FAILED");
            bestEffortDelete(attachment.getStorageKey());
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(Long requestId, AttachmentType type) {
        if (!repairRequestRepository.existsById(requestId)) {
            throw requestNotFound();
        }
        List<RepairAttachment> attachments = type == null
                ? attachmentRepository.findByRepairRequestIdAndStatusOrderByUploadedAtDesc(
                        requestId,
                        AttachmentStatus.AVAILABLE)
                : attachmentRepository.findByRepairRequestIdAndAttachmentTypeAndStatusOrderByUploadedAtDesc(
                        requestId,
                        type,
                        AttachmentStatus.AVAILABLE);
        return attachments.stream().map(AttachmentMapper::response).toList();
    }

    @Transactional(readOnly = true)
    public AttachmentResponse get(Long attachmentId) {
        return AttachmentMapper.response(availableAttachment(attachmentId));
    }

    @Transactional(readOnly = true)
    public DownloadUrlResponse downloadUrl(Long attachmentId) {
        RepairAttachment attachment = availableAttachment(attachmentId);
        OffsetDateTime expiresAt = now().plus(storageProperties.downloadUrlTtl());
        URI url = objectStorageService.createDownloadUrl(
                attachment.getStorageKey(),
                safeDownloadFileName(attachment),
                storageProperties.downloadUrlTtl());
        return new DownloadUrlResponse(url.toString(), expiresAt);
    }

    public void delete(Long attachmentId, AttachmentDeleteRequest request, AuthenticatedUser user) {
        String reason = validateDeletionReason(request == null ? null : request.reason());
        String storageKey = transactionTemplate.execute(status -> {
            Long requestId = attachmentRepository.findByIdWithRepairRequest(attachmentId)
                    .map(attachment -> attachment.getRepairRequest().getId())
                    .orElseThrow(this::attachmentNotFound);
            RepairRequest repairRequest = repairRequestRepository.findByIdForUpdate(requestId)
                    .orElseThrow(this::requestNotFound);
            RepairAttachment attachment = attachmentRepository.findByIdForUpdate(attachmentId)
                    .orElseThrow(this::attachmentNotFound);
            if (repairRequest.getStatus() == RepairRequestStatus.COMPLETED
                    || repairRequest.getStatus() == RepairRequestStatus.CANCELLED) {
                throw new BusinessRuleException(
                        "ATTACHMENT_DELETE_NOT_ALLOWED",
                        "Attachments on terminal requests cannot be deleted.",
                        409);
            }
            if (attachment.getStatus() == AttachmentStatus.DELETED) {
                throw new BusinessRuleException("ATTACHMENT_NOT_AVAILABLE", "Attachment is already deleted.", 409);
            }
            if (attachment.getStatus() != AttachmentStatus.AVAILABLE) {
                throw new BusinessRuleException("ATTACHMENT_NOT_AVAILABLE", "Attachment is not available.", 409);
            }
            attachment.markDeleted(user(user), reason, now());
            return attachment.getStorageKey();
        });
        bestEffortDelete(storageKey);
    }

    private RepairAttachment reserve(
            Long requestId,
            AttachmentType type,
            String originalFileName,
            UploadOwner owner) {
        return transactionTemplate.execute(status -> {
            RepairRequest request = repairRequestRepository.findByIdForUpdate(requestId).orElseThrow(this::requestNotFound);
            validateUploadAllowed(request.getStatus(), type);
            attachmentRepository.lockByRequestIdAndStatusIn(
                    requestId,
                    RepairAttachmentRepository.COUNTED_UPLOAD_STATUSES);
            long total = attachmentRepository.countByRepairRequestIdAndStatusIn(
                    requestId,
                    RepairAttachmentRepository.COUNTED_UPLOAD_STATUSES);
            if (total >= storageProperties.maxFilesPerRequest()) {
                throw limitExceeded();
            }
            long perType = attachmentRepository.countByRepairRequestIdAndAttachmentTypeAndStatusIn(
                    requestId,
                    type,
                    RepairAttachmentRepository.COUNTED_UPLOAD_STATUSES);
            if (perType >= storageProperties.maxFilesPerType()) {
                throw limitExceeded();
            }
            RepairAttachment attachment = switch (owner.kind()) {
                case STAFF -> new RepairAttachment(
                        request,
                        type,
                        storageKey(requestId, type),
                        originalFileName,
                        user(owner.staffUser),
                        now());
                case CUSTOMER -> RepairAttachment.customerUpload(
                            request,
                            type,
                            storageKey(requestId, type),
                            originalFileName,
                            customer(owner.customerId),
                            now());
                case TECHNICIAN -> RepairAttachment.technicianUpload(
                            request,
                            type,
                            storageKey(requestId, type),
                            originalFileName,
                            technician(owner.technicianId),
                            now());
            };
            try {
                return attachmentRepository.saveAndFlush(attachment);
            } catch (DataIntegrityViolationException exception) {
                throw new BusinessRuleException(
                        "ATTACHMENT_CONFLICT",
                        "Attachment state changed concurrently. Reload and try again.",
                        409);
            }
        });
    }

    private AttachmentResponse finalizeUpload(Long attachmentId, String contentType, long sizeBytes, String checksum) {
        return transactionTemplate.execute(status -> {
            Long requestId = attachmentRepository.findByIdWithRepairRequest(attachmentId)
                    .map(attachment -> attachment.getRepairRequest().getId())
                    .orElseThrow(this::attachmentNotFound);
            RepairRequest repairRequest = repairRequestRepository.findByIdForUpdate(requestId)
                    .orElseThrow(this::requestNotFound);
            RepairAttachment attachment = attachmentRepository.findByIdForUpdate(attachmentId)
                    .orElseThrow(this::attachmentNotFound);
            if (repairRequest.getStatus() == RepairRequestStatus.COMPLETED
                    || repairRequest.getStatus() == RepairRequestStatus.CANCELLED) {
                attachment.markFailed("REQUEST_TERMINAL", now());
                throw new BusinessRuleException(
                        "ATTACHMENT_UPLOAD_NOT_ALLOWED",
                        "Attachments cannot be made available on terminal requests.",
                        409);
            }
            if (!attachment.isUploading()) {
                throw new BusinessRuleException(
                        "ATTACHMENT_CONFLICT",
                        "Attachment upload was already finalized.",
                        409);
            }
            if (sizeBytes <= 0 || sizeBytes > storageProperties.maxFileSize().toBytes()) {
                attachment.markFailed("INVALID_SIZE", now());
                throw new BusinessRuleException(
                        "ATTACHMENT_FILE_TOO_LARGE",
                        "Attachment file exceeds the configured limit.",
                        400);
            }
            OffsetDateTime now = now();
            int updated = attachmentRepository.markAvailableIfUploading(
                    attachmentId,
                    contentType,
                    sizeBytes,
                    checksum,
                    now);
            if (updated != 1) {
                throw new BusinessRuleException(
                        "ATTACHMENT_CONFLICT",
                        "Attachment upload was already finalized.",
                        409);
            }
            RepairAttachment finalized = attachmentRepository.findByIdAndStatus(attachmentId, AttachmentStatus.AVAILABLE)
                    .orElseThrow(this::attachmentNotFound);
            return AttachmentMapper.response(finalized);
        });
    }

    private void failUpload(Long attachmentId, String reason) {
        transactionTemplate.executeWithoutResult(status -> attachmentRepository.markFailedIfUploading(
                attachmentId,
                safeFailure(reason),
                now()));
    }

    private RepairAttachment availableAttachment(Long attachmentId) {
        return attachmentRepository.findByIdAndStatus(attachmentId, AttachmentStatus.AVAILABLE)
                .orElseThrow(this::attachmentNotFound);
    }

    private void validateUploadAllowed(RepairRequestStatus status, AttachmentType type) {
        if (status == RepairRequestStatus.COMPLETED || status == RepairRequestStatus.CANCELLED) {
            throw new BusinessRuleException(
                    "ATTACHMENT_UPLOAD_NOT_ALLOWED",
                    "Attachments cannot be uploaded to terminal requests.",
                    409);
        }
        boolean allowed = switch (type) {
            case CUSTOMER_PROBLEM_PHOTO, GENERAL_DOCUMENT -> status == RepairRequestStatus.NEW
                    || status == RepairRequestStatus.ASSIGNED
                    || status == RepairRequestStatus.SCHEDULED
                    || status == RepairRequestStatus.IN_PROGRESS
                    || status == RepairRequestStatus.WAITING_FOR_PARTS;
            case DIAGNOSIS_PHOTO -> status == RepairRequestStatus.IN_PROGRESS
                    || status == RepairRequestStatus.WAITING_FOR_PARTS;
            case COMPLETION_PHOTO -> status == RepairRequestStatus.IN_PROGRESS;
        };
        if (!allowed) {
            throw new BusinessRuleException(
                    "ATTACHMENT_TYPE_NOT_ALLOWED",
                    "Attachment type is not allowed in the current request status.",
                    409);
        }
    }

    private void validateDeclaredContentType(String declaredContentType, String detectedContentType) {
        if (declaredContentType == null || declaredContentType.isBlank()) {
            return;
        }
        String normalized = declaredContentType.toLowerCase(Locale.ROOT);
        if (!normalized.equals(detectedContentType)) {
            throw new BusinessRuleException(
                    "ATTACHMENT_CONTENT_MISMATCH",
                    "Attachment declared content type does not match detected content.",
                    400);
        }
    }

    private String validateDeletionReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String trimmed = reason.trim();
        if (trimmed.length() > 1000) {
            throw new BusinessRuleException("VALIDATION_FAILED", "Deletion reason must be at most 1000 characters.", 400);
        }
        return trimmed;
    }

    private String safeDownloadFileName(RepairAttachment attachment) {
        String extension = validator.extensionFor(attachment.getContentType());
        String fallback = "repair-attachment-" + attachment.getId() + (extension == null ? "" : extension);
        String original = attachment.getOriginalFileName();
        if (original == null || original.isBlank() || original.indexOf('"') >= 0 || original.indexOf('\\') >= 0) {
            return fallback;
        }
        return original;
    }

    private String storageKey(Long requestId, AttachmentType type) {
        return "repair-requests/%d/%s/%s".formatted(
                requestId,
                type.name().toLowerCase(Locale.ROOT).replace('_', '-'),
                UUID.randomUUID());
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private Throwable rootCause(Throwable exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private void bestEffortDelete(String storageKey) {
        try {
            objectStorageService.delete(storageKey);
        } catch (StorageException exception) {
            // Metadata is already unavailable or failed; object cleanup can be retried operationally.
        }
    }

    private User user(AuthenticatedUser user) {
        return userRepository.findById(user.id()).orElseThrow(() -> new ResourceNotFoundException("User was not found."));
    }

    private Customer customer(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer was not found."));
    }

    private Technician technician(Long technicianId) {
        Technician technician = technicianRepository.findByIdForUpdate(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician was not found."));
        if (!technician.isActive()) {
            throw new BusinessRuleException(
                    "TECHNICIAN_INACTIVE",
                    "Inactive technician cannot upload attachments.",
                    409);
        }
        return technician;
    }

    private record UploadOwner(OwnerKind kind, AuthenticatedUser staffUser, Long customerId, Long technicianId) {
        static UploadOwner staff(AuthenticatedUser user) {
            return new UploadOwner(OwnerKind.STAFF, user, null, null);
        }

        static UploadOwner customer(Long customerId) {
            return new UploadOwner(OwnerKind.CUSTOMER, null, customerId, null);
        }

        static UploadOwner technician(Long technicianId) {
            return new UploadOwner(OwnerKind.TECHNICIAN, null, null, technicianId);
        }
    }

    private enum OwnerKind {
        STAFF,
        CUSTOMER,
        TECHNICIAN
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }

    private BusinessRuleException limitExceeded() {
        return new BusinessRuleException(
                "ATTACHMENT_LIMIT_EXCEEDED",
                "Attachment count limit exceeded.",
                409);
    }

    private ResourceNotFoundException requestNotFound() {
        return new ResourceNotFoundException("Repair request was not found.");
    }

    private BusinessRuleException attachmentNotFound() {
        return new BusinessRuleException("ATTACHMENT_NOT_FOUND", "Attachment was not found.", 404);
    }

    private String safeFailure(String reason) {
        if (reason.length() <= FAILURE_REASON_LIMIT) {
            return reason;
        }
        return reason.substring(0, FAILURE_REASON_LIMIT);
    }
}
