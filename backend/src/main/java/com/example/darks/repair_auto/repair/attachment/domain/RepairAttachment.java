package com.example.darks.repair_auto.repair.attachment.domain;

import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.technician.domain.Technician;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;

@Entity
@Table(name = "repair_attachments")
public class RepairAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repair_request_id")
    private RepairRequest repairRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_type", nullable = false, length = 40)
    private AttachmentType attachmentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttachmentStatus status;

    @Column(name = "storage_key", nullable = false, unique = true)
    private String storageKey;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "content_type", length = 80)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "sha256_checksum", length = 64)
    private String sha256Checksum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id")
    private User uploadedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_customer_id")
    private Customer uploadedByCustomer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_technician_id")
    private Technician uploadedByTechnician;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;

    @Column(name = "available_at")
    private OffsetDateTime availableAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by_user_id")
    private User deletedByUser;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "deletion_reason", length = 1000)
    private String deletionReason;

    @Column(name = "failure_reason", length = 120)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    private long version;

    protected RepairAttachment() {
    }

    public RepairAttachment(
            RepairRequest repairRequest,
            AttachmentType attachmentType,
            String storageKey,
            String originalFileName,
            User uploadedByUser,
            OffsetDateTime now) {
        this.repairRequest = repairRequest;
        this.attachmentType = attachmentType;
        this.status = AttachmentStatus.UPLOADING;
        this.storageKey = storageKey;
        this.originalFileName = originalFileName;
        this.uploadedByUser = uploadedByUser;
        this.uploadedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static RepairAttachment customerUpload(
            RepairRequest repairRequest,
            AttachmentType attachmentType,
            String storageKey,
            String originalFileName,
            Customer uploadedByCustomer,
            OffsetDateTime now) {
        RepairAttachment attachment = new RepairAttachment(
                repairRequest,
                attachmentType,
                storageKey,
                originalFileName,
                null,
                now);
        attachment.uploadedByCustomer = uploadedByCustomer;
        return attachment;
    }

    public static RepairAttachment technicianUpload(
            RepairRequest repairRequest,
            AttachmentType attachmentType,
            String storageKey,
            String originalFileName,
            Technician uploadedByTechnician,
            OffsetDateTime now) {
        RepairAttachment attachment = new RepairAttachment(
                repairRequest,
                attachmentType,
                storageKey,
                originalFileName,
                null,
                now);
        attachment.uploadedByTechnician = uploadedByTechnician;
        return attachment;
    }

    public Long getId() {
        return id;
    }

    public RepairRequest getRepairRequest() {
        return repairRequest;
    }

    public AttachmentType getAttachmentType() {
        return attachmentType;
    }

    public AttachmentStatus getStatus() {
        return status;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public String getSha256Checksum() {
        return sha256Checksum;
    }

    public User getUploadedByUser() {
        return uploadedByUser;
    }

    public Customer getUploadedByCustomer() {
        return uploadedByCustomer;
    }

    public Technician getUploadedByTechnician() {
        return uploadedByTechnician;
    }

    public OffsetDateTime getUploadedAt() {
        return uploadedAt;
    }

    public OffsetDateTime getAvailableAt() {
        return availableAt;
    }

    public User getDeletedByUser() {
        return deletedByUser;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public String getDeletionReason() {
        return deletionReason;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isAvailable() {
        return status == AttachmentStatus.AVAILABLE;
    }

    public boolean isUploading() {
        return status == AttachmentStatus.UPLOADING;
    }

    public void markAvailable(
            String detectedContentType,
            long detectedSizeBytes,
            String checksum,
            OffsetDateTime now) {
        this.status = AttachmentStatus.AVAILABLE;
        this.contentType = detectedContentType;
        this.sizeBytes = detectedSizeBytes;
        this.sha256Checksum = checksum;
        this.availableAt = now;
        this.failureReason = null;
        this.updatedAt = now;
    }

    public void markFailed(String safeReason, OffsetDateTime now) {
        this.status = AttachmentStatus.FAILED;
        this.failureReason = safeReason;
        this.updatedAt = now;
    }

    public void markDeleted(User deletedBy, String reason, OffsetDateTime now) {
        this.status = AttachmentStatus.DELETED;
        this.deletedByUser = deletedBy;
        this.deletedAt = now;
        this.deletionReason = reason;
        this.updatedAt = now;
    }
}
