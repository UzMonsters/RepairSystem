package com.example.darks.repair_auto.notification.infrastructure.persistence;

import com.example.darks.repair_auto.notification.domain.NotificationPushDelivery;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationPushDeliveryRepository extends JpaRepository<NotificationPushDelivery, Long> {

    Optional<NotificationPushDelivery> findByNotificationOutboxIdAndPushEndpointId(
            Long notificationOutboxId,
            Long pushEndpointId);

    @Query("select d from NotificationPushDelivery d where d.notificationOutbox.id = :outboxId")
    List<NotificationPushDelivery> findByNotificationOutboxId(@Param("outboxId") Long outboxId);
}
