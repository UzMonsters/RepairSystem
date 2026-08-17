package com.example.darks.repair_auto.notification.application;

import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.notification.api.dto.NotificationDeliveryResponse;
import com.example.darks.repair_auto.notification.api.dto.NotificationMapper;
import com.example.darks.repair_auto.notification.api.dto.NotificationSummaryResponse;
import com.example.darks.repair_auto.notification.domain.NotificationOutbox;
import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
import com.example.darks.repair_auto.notification.domain.NotificationStatus;
import com.example.darks.repair_auto.notification.infrastructure.persistence.NotificationDeliveryAttemptRepository;
import com.example.darks.repair_auto.notification.infrastructure.persistence.NotificationOutboxRepository;
import com.example.darks.repair_auto.notification.infrastructure.worker.NotificationWorkerTransactions;
import com.example.darks.repair_auto.settings.domain.Language;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
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
    private final NotificationTemplateService templateService;
    private final CustomerRepository customerRepository;
    private final TechnicianRepository technicianRepository;

    public NotificationAdminService(
            NotificationOutboxRepository outboxRepository,
            NotificationDeliveryAttemptRepository attemptRepository,
            NotificationWorkerTransactions workerTransactions,
            NotificationTemplateService templateService,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository) {
        this.outboxRepository = outboxRepository;
        this.attemptRepository = attemptRepository;
        this.workerTransactions = workerTransactions;
        this.templateService = templateService;
        this.customerRepository = customerRepository;
        this.technicianRepository = technicianRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationSummaryResponse> list(NotificationQuery query, Pageable pageable, Language language) {
        validateDateRange(query.createdFrom(), query.createdTo());
        return PageResponse.from(outboxRepository.findAll(filters(query), pageable)
                .map(notification -> NotificationMapper.summary(
                        notification,
                        recipientName(notification.getRecipientType(), notification.getRecipientId()),
                        toLanguageCode(language),
                        templateService)));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDeliveryResponse> listDeliveries(NotificationQuery query, Pageable pageable) {
        validateDateRange(query.createdFrom(), query.createdTo());
        return PageResponse.from(outboxRepository.findAll(filters(query), pageable)
                .map(notification -> NotificationMapper.delivery(
                        notification,
                        attemptRepository.findByNotificationIdOrderByAttemptNumberDesc(notification.getId()))));
    }

    @Transactional(readOnly = true)
    public NotificationDeliveryResponse getDelivery(Long id) {
        NotificationOutbox notification = outboxRepository.findWithRelationsById(id).orElseThrow(this::notFound);
        return NotificationMapper.delivery(
                notification,
                attemptRepository.findByNotificationIdOrderByAttemptNumberDesc(id));
    }

    public NotificationDeliveryResponse retry(Long id, String reason) {
        String safeReason = validateReason(reason);
        workerTransactions.retry(id, safeReason);
        return getDelivery(id);
    }

    private Specification<NotificationOutbox> filters(NotificationQuery query) {
        return (root, criteriaQuery, builder) -> {
            var predicate = builder.conjunction();
            if (query.status() != null) {
                if (query.status() == NotificationStatus.FAILED) {
                    predicate = builder.and(predicate, builder.equal(root.get("status"), NotificationStatus.DEAD));
                } else {
                    predicate = builder.and(predicate, builder.equal(root.get("status"), query.status()));
                }
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

    private String recipientName(NotificationRecipientType recipientType, Long recipientId) {
        if (recipientType == NotificationRecipientType.CUSTOMER) {
            return customerRepository.findById(recipientId).map(customer -> customer.getFullName()).orElse(null);
        }
        return technicianRepository.findById(recipientId).map(technician -> technician.getFullName()).orElse(null);
    }

    private LanguageCode toLanguageCode(Language language) {
        if (language == null) {
            return LanguageCode.UZ;
        }
        return LanguageCode.valueOf(language.name());
    }
}
