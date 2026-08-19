package com.example.darks.repair_auto.notification.inbox.infrastructure;

import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
import com.example.darks.repair_auto.notification.inbox.domain.UserNotification;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    Optional<UserNotification> findByEventKeyAndRecipientType(String eventKey, NotificationRecipientType recipientType);

    Page<UserNotification> findByCustomerId(Long customerId, Pageable pageable);

    Page<UserNotification> findByCustomerIdAndReadAtIsNull(Long customerId, Pageable pageable);

    Page<UserNotification> findByCustomerIdAndReadAtIsNotNull(Long customerId, Pageable pageable);

    Page<UserNotification> findByTechnicianId(Long technicianId, Pageable pageable);

    Page<UserNotification> findByTechnicianIdAndReadAtIsNull(Long technicianId, Pageable pageable);

    Page<UserNotification> findByTechnicianIdAndReadAtIsNotNull(Long technicianId, Pageable pageable);

    long countByCustomerIdAndReadAtIsNull(Long customerId);

    long countByTechnicianIdAndReadAtIsNull(Long technicianId);

    @Modifying
    @Query("""
            update UserNotification un
            set un.readAt = :readAt, un.updatedAt = :readAt
            where un.customer.id = :customerId
            and un.readAt is null
            """)
    int markAllAsReadForCustomer(@Param("customerId") Long customerId, @Param("readAt") OffsetDateTime readAt);

    @Modifying
    @Query("""
            update UserNotification un
            set un.readAt = :readAt, un.updatedAt = :readAt
            where un.technician.id = :technicianId
            and un.readAt is null
            """)
    int markAllAsReadForTechnician(@Param("technicianId") Long technicianId, @Param("readAt") OffsetDateTime readAt);

    @Modifying
    @Query(value = """
            insert into user_notifications (
                event_key,
                notification_type,
                recipient_type,
                customer_id,
                technician_id,
                repair_request_id,
                request_number,
                target,
                target_id,
                payload_json,
                read_at,
                created_at,
                updated_at,
                version
            ) values (
                :eventKey,
                :notificationType,
                'CUSTOMER',
                :customerId,
                null,
                :repairRequestId,
                :requestNumber,
                :target,
                :targetId,
                :payloadJson,
                null,
                :createdAt,
                :createdAt,
                0
            )
            on conflict (event_key, customer_id) where recipient_type = 'CUSTOMER'
            do nothing
            """, nativeQuery = true)
    int insertForCustomerOnConflictDoNothing(
            @Param("eventKey") String eventKey,
            @Param("notificationType") String notificationType,
            @Param("customerId") Long customerId,
            @Param("repairRequestId") Long repairRequestId,
            @Param("requestNumber") String requestNumber,
            @Param("target") String target,
            @Param("targetId") Long targetId,
            @Param("payloadJson") String payloadJson,
            @Param("createdAt") OffsetDateTime createdAt);

    @Modifying
    @Query(value = """
            insert into user_notifications (
                event_key,
                notification_type,
                recipient_type,
                customer_id,
                technician_id,
                repair_request_id,
                request_number,
                target,
                target_id,
                payload_json,
                read_at,
                created_at,
                updated_at,
                version
            ) values (
                :eventKey,
                :notificationType,
                'TECHNICIAN',
                null,
                :technicianId,
                :repairRequestId,
                :requestNumber,
                :target,
                :targetId,
                :payloadJson,
                null,
                :createdAt,
                :createdAt,
                0
            )
            on conflict (event_key, technician_id) where recipient_type = 'TECHNICIAN'
            do nothing
            """, nativeQuery = true)
    int insertForTechnicianOnConflictDoNothing(
            @Param("eventKey") String eventKey,
            @Param("notificationType") String notificationType,
            @Param("technicianId") Long technicianId,
            @Param("repairRequestId") Long repairRequestId,
            @Param("requestNumber") String requestNumber,
            @Param("target") String target,
            @Param("targetId") Long targetId,
            @Param("payloadJson") String payloadJson,
            @Param("createdAt") OffsetDateTime createdAt);
}
