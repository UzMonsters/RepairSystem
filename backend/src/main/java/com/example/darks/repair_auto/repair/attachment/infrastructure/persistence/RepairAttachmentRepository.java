package com.example.darks.repair_auto.repair.attachment.infrastructure.persistence;

import com.example.darks.repair_auto.repair.attachment.domain.AttachmentStatus;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.repair.attachment.domain.RepairAttachment;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface RepairAttachmentRepository extends JpaRepository<RepairAttachment, Long> {

    Collection<AttachmentStatus> BUSINESS_VISIBLE_STATUSES = List.of(AttachmentStatus.AVAILABLE);
    Collection<AttachmentStatus> COUNTED_UPLOAD_STATUSES = List.of(AttachmentStatus.UPLOADING, AttachmentStatus.AVAILABLE);

    @Query("""
            select a from RepairAttachment a
            join fetch a.repairRequest
            left join fetch a.uploadedByUser
            left join fetch a.uploadedByCustomer
            where a.repairRequest.id = :requestId and a.status = :status
            order by a.uploadedAt desc
            """)
    List<RepairAttachment> findByRepairRequestIdAndStatusOrderByUploadedAtDesc(
            @Param("requestId") Long requestId,
            @Param("status") AttachmentStatus status);

    @Query("""
            select a from RepairAttachment a
            join fetch a.repairRequest
            left join fetch a.uploadedByUser
            left join fetch a.uploadedByCustomer
            where a.repairRequest.id = :requestId
                and a.attachmentType = :attachmentType
                and a.status = :status
            order by a.uploadedAt desc
            """)
    List<RepairAttachment> findByRepairRequestIdAndAttachmentTypeAndStatusOrderByUploadedAtDesc(
            @Param("requestId") Long requestId,
            @Param("attachmentType") AttachmentType attachmentType,
            @Param("status") AttachmentStatus status);

    @Query("""
            select a from RepairAttachment a
            join fetch a.repairRequest
            left join fetch a.uploadedByUser
            left join fetch a.uploadedByCustomer
            where a.id = :id and a.status = :status
            """)
    Optional<RepairAttachment> findByIdAndStatus(@Param("id") Long id, @Param("status") AttachmentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    @Query("""
            select a from RepairAttachment a
            join fetch a.repairRequest
            left join fetch a.uploadedByUser
            left join fetch a.uploadedByCustomer
            where a.id = :id
            """)
    Optional<RepairAttachment> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select a from RepairAttachment a
            join fetch a.repairRequest
            where a.id = :id
            """)
    Optional<RepairAttachment> findByIdWithRepairRequest(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    @Query("""
            select a from RepairAttachment a
            where a.repairRequest.id = :requestId
                and a.attachmentType = :attachmentType
                and a.status in :statuses
            order by a.id
            """)
    List<RepairAttachment> lockByRequestIdAndAttachmentTypeAndStatusIn(
            @Param("requestId") Long requestId,
            @Param("attachmentType") AttachmentType attachmentType,
            @Param("statuses") Collection<AttachmentStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    @Query("""
            select a from RepairAttachment a
            where a.repairRequest.id = :requestId
                and a.status in :statuses
            order by a.id
            """)
    List<RepairAttachment> lockByRequestIdAndStatusIn(
            @Param("requestId") Long requestId,
            @Param("statuses") Collection<AttachmentStatus> statuses);

    long countByRepairRequestIdAndStatusIn(Long requestId, Collection<AttachmentStatus> statuses);

    long countByRepairRequestIdAndAttachmentTypeAndStatusIn(
            Long requestId,
            AttachmentType attachmentType,
            Collection<AttachmentStatus> statuses);

    long countByRepairRequestIdAndAttachmentTypeAndStatus(
            Long requestId,
            AttachmentType attachmentType,
            AttachmentStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RepairAttachment a
            set a.status = com.example.darks.repair_auto.repair.attachment.domain.AttachmentStatus.AVAILABLE,
                a.contentType = :contentType,
                a.sizeBytes = :sizeBytes,
                a.sha256Checksum = :checksum,
                a.availableAt = :now,
                a.failureReason = null,
                a.updatedAt = :now
            where a.id = :attachmentId
                and a.status = com.example.darks.repair_auto.repair.attachment.domain.AttachmentStatus.UPLOADING
            """)
    int markAvailableIfUploading(
            @Param("attachmentId") Long attachmentId,
            @Param("contentType") String contentType,
            @Param("sizeBytes") long sizeBytes,
            @Param("checksum") String checksum,
            @Param("now") java.time.OffsetDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RepairAttachment a
            set a.status = com.example.darks.repair_auto.repair.attachment.domain.AttachmentStatus.FAILED,
                a.failureReason = :reason,
                a.updatedAt = :now
            where a.id = :attachmentId
                and a.status = com.example.darks.repair_auto.repair.attachment.domain.AttachmentStatus.UPLOADING
            """)
    int markFailedIfUploading(
            @Param("attachmentId") Long attachmentId,
            @Param("reason") String reason,
            @Param("now") java.time.OffsetDateTime now);
}
