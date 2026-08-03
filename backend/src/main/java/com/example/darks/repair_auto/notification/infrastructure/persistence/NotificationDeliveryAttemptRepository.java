package com.example.darks.repair_auto.notification.infrastructure.persistence;

import com.example.darks.repair_auto.notification.domain.NotificationDeliveryAttempt;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryAttemptRepository
        extends JpaRepository<NotificationDeliveryAttempt, Long> {

    @EntityGraph(attributePaths = {"notification"})
    List<NotificationDeliveryAttempt> findByNotificationIdOrderByAttemptNumberDesc(Long notificationId);
}
