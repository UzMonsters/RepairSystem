package com.example.darks.repair_auto.notification.application;

import com.example.darks.repair_auto.notification.api.dto.NotificationDetailResponse;
import com.example.darks.repair_auto.notification.api.dto.NotificationMapper;
import com.example.darks.repair_auto.notification.api.dto.NotificationSummaryResponse;
import com.example.darks.repair_auto.notification.domain.NotificationOutbox;
import com.example.darks.repair_auto.notification.infrastructure.persistence.NotificationDeliveryAttemptRepository;
import com.example.darks.repair_auto.notification.infrastructure.persistence.NotificationOutboxRepository;
import com.example.darks.repair_auto.notification.infrastructure.worker.NotificationWorkerTransactions;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationAdminService {

    private static final int MAX_REASON_LENGTH = 500;

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationDeliveryAttemptRepository attemptRepository;
    private final NotificationWorkerTransactions workerTransactions;

    public NotificationAdminService(
            NotificationOutboxRepository outboxRepository,
            NotificationDeliveryAttemptRepository attemptRepository,
            NotificationWorkerTransactions workerTransactions) {
        this.outboxRepository = outboxRepository;
        this.attemptRepository = attemptRepository;
        this.workerTransactions = workerTransactions;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationSummaryResponse> list(NotificationQuery query, Pageable pageable) {
        validateDateRange(query.createdFrom(), query.createdTo());
        return PageResponse.from(outboxRepository.findAll(filters(query), pageable)
                .map(NotificationMapper::summary));
    }

    @Transactional(readOnly = true)
    public NotificationDetailResponse get(Long id) {
        NotificationOutbox notification = outboxRepository.findWithRelationsById(id).orElseThrow(this::notFound);
        return NotificationMapper.detail(
                notification,
                attemptRepository.findByNotificationIdOrderByAttemptNumberDesc(id));
    }

    public NotificationDetailResponse retry(Long id, String reason) {
        String safeReason = validateReason(reason);
        workerTransactions.retry(id, safeReason);
        return get(id);
    }

    private Specification<NotificationOutbox> filters(NotificationQuery query) {
        return (root, criteriaQuery, builder) -> {
            var predicate = builder.conjunction();
            if (query.status() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("status"), query.status()));
            }
            if (query.notificationType() != null) {
                predicate = builder.and(
                        predicate,
                        builder.equal(root.get("notificationType"), query.notificationType()));
            }
            if (query.recipientType() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("recipientType"), query.recipientType()));
            }
            if (query.repairRequestId() != null) {
                predicate = builder.and(
                        predicate,
                        builder.equal(root.get("repairRequest").get("id"), query.repairRequestId()));
            }
            if (query.createdFrom() != null) {
                predicate = builder.and(
                        predicate,
                        builder.greaterThanOrEqualTo(root.get("createdAt"), query.createdFrom()));
            }
            if (query.createdTo() != null) {
                predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("createdAt"), query.createdTo()));
            }
            return predicate;
        };
    }

    private String validateReason(String value) {
        if (value == null || value.isBlank() || value.trim().length() > MAX_REASON_LENGTH) {
            throw new BusinessRuleException(
                    "VALIDATION_FAILED",
                    "Retry reason must be between 1 and 500 characters.",
                    400);
        }
        return value.trim();
    }

    private void validateDateRange(java.time.OffsetDateTime from, java.time.OffsetDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleException(
                    "INVALID_NOTIFICATION_DATE_RANGE",
                    "createdFrom must be before or equal to createdTo.",
                    400);
        }
    }

    private BusinessRuleException notFound() {
        return new BusinessRuleException("NOTIFICATION_NOT_FOUND", "Notification was not found.", 404);
    }
}
