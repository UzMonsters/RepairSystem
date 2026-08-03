package com.example.darks.repair_auto.notification.infrastructure.persistence;

import com.example.darks.repair_auto.notification.domain.NotificationOutbox;
import com.example.darks.repair_auto.notification.domain.NotificationStatus;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationOutboxRepository
        extends JpaRepository<NotificationOutbox, Long>, JpaSpecificationExecutor<NotificationOutbox> {

    Optional<NotificationOutbox> findByEventKey(String eventKey);

    long countByEventKey(String eventKey);

    long countByStatus(NotificationStatus status);

    List<NotificationOutbox> findByEventKeyIn(Collection<String> eventKeys);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"repairRequest", "repairRequest.customer", "repairRequest.category"})
    @Query("select n from NotificationOutbox n where n.id = :id")
    Optional<NotificationOutbox> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"repairRequest", "repairRequest.customer", "repairRequest.category"})
    @Query("select n from NotificationOutbox n where n.id = :id")
    Optional<NotificationOutbox> findWithRelationsById(@Param("id") Long id);

    @Query(value = """
            select *
            from notification_outbox
            where id in (
                select id
                from notification_outbox
                where (
                    status in ('PENDING', 'RETRY_SCHEDULED') and next_attempt_at <= :now
                ) or (
                    status = 'PROCESSING' and processing_lease_until < :now
                )
                order by created_at, id
                for update skip locked
                limit :limit
            )
            order by created_at, id
            """, nativeQuery = true)
    List<NotificationOutbox> findClaimableForUpdate(
            @Param("now") OffsetDateTime now,
            @Param("limit") int limit);
}
