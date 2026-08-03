package com.example.darks.repair_auto.notification.infrastructure.worker;

import com.example.darks.repair_auto.notification.application.ClaimedNotification;
import com.example.darks.repair_auto.notification.application.NotificationDeliveryResult;
import com.example.darks.repair_auto.notification.application.NotificationRetryPolicy;
import com.example.darks.repair_auto.notification.domain.NotificationAttemptOutcome;
import com.example.darks.repair_auto.notification.domain.NotificationDeliveryAttempt;
import com.example.darks.repair_auto.notification.domain.NotificationFailureCategory;
import com.example.darks.repair_auto.notification.domain.NotificationOutbox;
import com.example.darks.repair_auto.notification.domain.NotificationStatus;
import com.example.darks.repair_auto.notification.infrastructure.persistence.NotificationDeliveryAttemptRepository;
import com.example.darks.repair_auto.notification.infrastructure.persistence.NotificationOutboxRepository;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationWorkerTransactions {

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationDeliveryAttemptRepository attemptRepository;
    private final NotificationProperties properties;
    private final NotificationRetryPolicy retryPolicy;
    private final Clock clock;

    @Autowired
    public NotificationWorkerTransactions(
            NotificationOutboxRepository outboxRepository,
            NotificationDeliveryAttemptRepository attemptRepository,
            NotificationProperties properties,
            NotificationRetryPolicy retryPolicy) {
        this(outboxRepository, attemptRepository, properties, retryPolicy, Clock.systemUTC());
    }

    NotificationWorkerTransactions(
            NotificationOutboxRepository outboxRepository,
            NotificationDeliveryAttemptRepository attemptRepository,
            NotificationProperties properties,
            NotificationRetryPolicy retryPolicy,
            Clock clock) {
        this.outboxRepository = outboxRepository;
        this.attemptRepository = attemptRepository;
        this.properties = properties;
        this.retryPolicy = retryPolicy;
        this.clock = clock;
    }

    @Transactional
    public List<ClaimedNotification> claim(String workerId) {
        OffsetDateTime now = now();
        List<NotificationOutbox> claimable =
                outboxRepository.findClaimableForUpdate(now, properties.getBatchSize());
        for (NotificationOutbox notification : claimable) {
            if (notification.getStatus() == NotificationStatus.PROCESSING) {
                notification.recoverLease(now);
                notification.incrementAttemptCount();
                attemptRepository.save(new NotificationDeliveryAttempt(
                        notification,
                        notification.getAttemptCount(),
                        workerId,
                        now,
                        now,
                        NotificationAttemptOutcome.LEASE_RECOVERED,
                        NotificationFailureCategory.PROCESSING_LEASE_EXPIRED,
                        null));
            }
            notification.claim(workerId, now, now.plus(properties.getProcessingLease()));
        }
        return claimable.stream()
                .map(notification -> new ClaimedNotification(
                        notification.getId(),
                        notification.getNotificationType(),
                        notification.getRecipientType(),
                        notification.getRecipientId(),
                        notification.getTemplateKey(),
                        notification.getPayloadJson(),
                        notification.getPayloadVersion()))
                .toList();
    }

    @Transactional
    public void finalizeDelivery(
            Long notificationId,
            String workerId,
            OffsetDateTime startedAt,
            NotificationDeliveryResult result) {
        OffsetDateTime now = now();
        NotificationOutbox notification = outboxRepository.findByIdForUpdate(notificationId)
                .orElseThrow();
        if (notification.getStatus() != NotificationStatus.PROCESSING) {
            return;
        }
        notification.incrementAttemptCount();
        int attemptNumber = notification.getAttemptCount();
        if (result.outcome() == NotificationAttemptOutcome.DELIVERED) {
            notification.markDelivered(result.providerMessageId(), now);
        } else if (result.outcome() == NotificationAttemptOutcome.RECIPIENT_UNAVAILABLE) {
            notification.markSkipped(result.failureCategory(), now);
        } else if (result.outcome() == NotificationAttemptOutcome.PERMANENT_FAILURE) {
            notification.markDead(result.failureCategory(), now);
        } else if (attemptNumber >= properties.getMaxAttempts()) {
            notification.markDead(NotificationFailureCategory.MAX_ATTEMPTS_EXHAUSTED, now);
        } else {
            notification.scheduleRetry(result.failureCategory(), now.plus(retryPolicy.nextBackoff(attemptNumber)), now);
        }
        attemptRepository.save(new NotificationDeliveryAttempt(
                notification,
                attemptNumber,
                workerId,
                startedAt,
                now,
                result.outcome(),
                result.failureCategory(),
                result.providerMessageId()));
    }

    @Transactional
    public void retry(Long notificationId, String reason) {
        OffsetDateTime now = now();
        NotificationOutbox notification = outboxRepository.findByIdForUpdate(notificationId)
                .orElseThrow(() -> new BusinessRuleException(
                        "NOTIFICATION_NOT_FOUND",
                        "Notification was not found.",
                        404));
        if (notification.getStatus() == NotificationStatus.DELIVERED) {
            throw new BusinessRuleException(
                    "NOTIFICATION_ALREADY_DELIVERED",
                    "Delivered notifications cannot be retried.",
                    409);
        }
        if (notification.getStatus() == NotificationStatus.PENDING
                || notification.getStatus() == NotificationStatus.PROCESSING) {
            throw new BusinessRuleException(
                    "NOTIFICATION_RETRY_NOT_ALLOWED",
                    "Only failed or skipped notifications can be retried.",
                    409);
        }
        notification.manualRetry(reason, now);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }
}
