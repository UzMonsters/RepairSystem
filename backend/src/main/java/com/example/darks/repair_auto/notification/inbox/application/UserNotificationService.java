package com.example.darks.repair_auto.notification.inbox.application;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.notification.application.NotificationEventFactory;
import com.example.darks.repair_auto.notification.application.NotificationTemplateService;
import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
import com.example.darks.repair_auto.notification.inbox.api.dto.UnreadNotificationCountResponse;
import com.example.darks.repair_auto.notification.inbox.api.dto.UserNotificationResponse;
import com.example.darks.repair_auto.notification.inbox.domain.UserNotification;
import com.example.darks.repair_auto.notification.inbox.infrastructure.UserNotificationRepository;
import com.example.darks.repair_auto.shared.error.ResourceNotFoundException;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.shared.i18n.RequestLocaleResolver;
import com.example.darks.repair_auto.shared.i18n.SupportedLanguage;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserNotificationService.class);

    public enum RecordResult {
        CREATED,
        ALREADY_EXISTS,
        SKIPPED
    }

    private final UserNotificationRepository userNotificationRepository;
    private final CustomerRepository customerRepository;
    private final TechnicianRepository technicianRepository;
    private final NotificationTemplateService templateService;
    private final RequestLocaleResolver requestLocaleResolver;
    private final org.springframework.context.ApplicationEventPublisher applicationEventPublisher;
    private final Clock clock;

    @Autowired
    public UserNotificationService(
            UserNotificationRepository userNotificationRepository,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            NotificationTemplateService templateService,
            RequestLocaleResolver requestLocaleResolver,
            org.springframework.context.ApplicationEventPublisher applicationEventPublisher) {
        this(userNotificationRepository, customerRepository, technicianRepository, templateService, requestLocaleResolver, applicationEventPublisher, Clock.systemUTC());
    }

    public UserNotificationService(
            UserNotificationRepository userNotificationRepository,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            NotificationTemplateService templateService,
            RequestLocaleResolver requestLocaleResolver,
            Clock clock) {
        this(userNotificationRepository, customerRepository, technicianRepository, templateService, requestLocaleResolver, null, clock);
    }

    public UserNotificationService(
            UserNotificationRepository userNotificationRepository,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            NotificationTemplateService templateService,
            RequestLocaleResolver requestLocaleResolver,
            org.springframework.context.ApplicationEventPublisher applicationEventPublisher,
            Clock clock) {
        this.userNotificationRepository = userNotificationRepository;
        this.customerRepository = customerRepository;
        this.technicianRepository = technicianRepository;
        this.templateService = templateService;
        this.requestLocaleResolver = requestLocaleResolver;
        this.applicationEventPublisher = applicationEventPublisher;
        this.clock = clock;
    }

    @Transactional
    public RecordResult recordFromEvent(NotificationEventFactory.NotificationEvent event) {
        if (event.recipientType() == NotificationRecipientType.STAFF) {
            return RecordResult.SKIPPED;
        }

        OffsetDateTime now = now();
        String requestNumber = event.repairRequest() != null ? event.repairRequest().getRequestNumber() : null;
        Long repairRequestId = event.repairRequest() != null ? event.repairRequest().getId() : null;
        Long targetId = repairRequestId;

        if (event.recipientType() == NotificationRecipientType.CUSTOMER) {
            Customer customer = customerRepository.findById(event.recipientId()).orElse(null);
            if (customer == null) {
                LOGGER.warn("Skipping inbox notification creation: customer id={} not found", event.recipientId());
                return RecordResult.SKIPPED;
            }
            String target = "REPAIR_REQUEST_DETAILS";
            int inserted = userNotificationRepository.insertForCustomerOnConflictDoNothing(
                    event.eventKey(),
                    event.type().name(),
                    customer.getId(),
                    repairRequestId,
                    requestNumber,
                    target,
                    targetId,
                    event.payloadJson(),
                    now);
            if (inserted > 0) {
                publishDomainEvent(new com.example.darks.repair_auto.realtime.event.application.NotificationCreatedDomainEvent(
                        com.example.darks.repair_auto.identity.domain.ActorType.CUSTOMER,
                        customer.getId(),
                        null,
                        event.type().name(),
                        targetId,
                        target));
                return RecordResult.CREATED;
            }
            return RecordResult.ALREADY_EXISTS;
        } else if (event.recipientType() == NotificationRecipientType.TECHNICIAN) {
            Technician technician = technicianRepository.findById(event.recipientId()).orElse(null);
            if (technician == null) {
                LOGGER.warn("Skipping inbox notification creation: technician id={} not found", event.recipientId());
                return RecordResult.SKIPPED;
            }
            String target = "TECHNICIAN_JOB_DETAILS";
            int inserted = userNotificationRepository.insertForTechnicianOnConflictDoNothing(
                    event.eventKey(),
                    event.type().name(),
                    technician.getId(),
                    repairRequestId,
                    requestNumber,
                    target,
                    targetId,
                    event.payloadJson(),
                    now);
            if (inserted > 0) {
                publishDomainEvent(new com.example.darks.repair_auto.realtime.event.application.NotificationCreatedDomainEvent(
                        com.example.darks.repair_auto.identity.domain.ActorType.TECHNICIAN,
                        technician.getId(),
                        null,
                        event.type().name(),
                        targetId,
                        target));
                return RecordResult.CREATED;
            }
            return RecordResult.ALREADY_EXISTS;
        }

        return RecordResult.SKIPPED;
    }

    @Transactional(readOnly = true)
    public Page<UserNotificationResponse> listForMobile(
            AuthenticatedMobileActor actor,
            Pageable pageable,
            Boolean unreadOnly) {
        Page<UserNotification> page;
        if (actor.isCustomer()) {
            if (Boolean.TRUE.equals(unreadOnly)) {
                page = userNotificationRepository.findByCustomerIdAndReadAtIsNull(actor.actorId(), pageable);
            } else if (Boolean.FALSE.equals(unreadOnly)) {
                page = userNotificationRepository.findByCustomerIdAndReadAtIsNotNull(actor.actorId(), pageable);
            } else {
                page = userNotificationRepository.findByCustomerId(actor.actorId(), pageable);
            }
        } else if (actor.isTechnician()) {
            if (Boolean.TRUE.equals(unreadOnly)) {
                page = userNotificationRepository.findByTechnicianIdAndReadAtIsNull(actor.actorId(), pageable);
            } else if (Boolean.FALSE.equals(unreadOnly)) {
                page = userNotificationRepository.findByTechnicianIdAndReadAtIsNotNull(actor.actorId(), pageable);
            } else {
                page = userNotificationRepository.findByTechnicianId(actor.actorId(), pageable);
            }
        } else {
            throw new AccessDeniedException("Staff actor cannot access mobile notification inbox.");
        }

        LanguageCode language = resolveLanguageCode();
        return page.map(notification -> mapToResponse(notification, language));
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getUnreadCount(AuthenticatedMobileActor actor) {
        long count;
        if (actor.isCustomer()) {
            count = userNotificationRepository.countByCustomerIdAndReadAtIsNull(actor.actorId());
        } else if (actor.isTechnician()) {
            count = userNotificationRepository.countByTechnicianIdAndReadAtIsNull(actor.actorId());
        } else {
            throw new AccessDeniedException("Staff actor cannot access mobile notification inbox.");
        }
        return new UnreadNotificationCountResponse(count);
    }

    @Transactional
    public void markAsRead(AuthenticatedMobileActor actor, Long notificationId) {
        UserNotification notification = userNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification was not found."));

        if (actor.isCustomer()) {
            if (notification.getCustomer() == null || !notification.getCustomer().getId().equals(actor.actorId())) {
                throw new ResourceNotFoundException("Notification was not found.");
            }
        } else if (actor.isTechnician()) {
            if (notification.getTechnician() == null || !notification.getTechnician().getId().equals(actor.actorId())) {
                throw new ResourceNotFoundException("Notification was not found.");
            }
        } else {
            throw new AccessDeniedException("Staff actor cannot mark mobile notification as read.");
        }

        if (notification.getReadAt() == null) {
            notification.markRead(now());
            userNotificationRepository.save(notification);
            publishDomainEvent(new com.example.darks.repair_auto.realtime.event.application.NotificationReadDomainEvent(
                    actor.actorType(),
                    actor.actorId(),
                    notification.getId()));
        }
    }

    @Transactional
    public void markAllAsRead(AuthenticatedMobileActor actor) {
        OffsetDateTime now = now();
        if (actor.isCustomer()) {
            userNotificationRepository.markAllAsReadForCustomer(actor.actorId(), now);
        } else if (actor.isTechnician()) {
            userNotificationRepository.markAllAsReadForTechnician(actor.actorId(), now);
        } else {
            throw new AccessDeniedException("Staff actor cannot mark mobile notifications as read.");
        }
        publishDomainEvent(new com.example.darks.repair_auto.realtime.event.application.NotificationReadDomainEvent(
                actor.actorType(),
                actor.actorId(),
                null));
    }

    private void publishDomainEvent(Object event) {
        if (applicationEventPublisher != null) {
            applicationEventPublisher.publishEvent(event);
        }
    }

    public UserNotificationResponse mapToResponse(UserNotification notification, LanguageCode language) {
        String title;
        String body;
        try {
            var rendered = templateService.render(
                    notification.getNotificationType(),
                    notification.getRecipientType(),
                    notification.getPayloadJson(),
                    1,
                    language);
            title = rendered.title();
            body = rendered.message();
        } catch (Exception e) {
            LOGGER.warn("Failed to render notification template for id={}, falling back to UZ: {}",
                    notification.getId(), e.getMessage());
            try {
                var fallback = templateService.render(
                        notification.getNotificationType(),
                        notification.getRecipientType(),
                        notification.getPayloadJson(),
                        1,
                        LanguageCode.UZ);
                title = fallback.title();
                body = fallback.message();
            } catch (Exception ex) {
                title = notification.getNotificationType().name();
                body = notification.getRequestNumber() != null ? notification.getRequestNumber() : "";
            }
        }

        return new UserNotificationResponse(
                notification.getId(),
                notification.getNotificationType(),
                title,
                body,
                notification.isRead(),
                notification.getReadAt(),
                notification.getTarget(),
                notification.getTargetId(),
                notification.getRequestNumber(),
                notification.getCreatedAt());
    }

    private LanguageCode resolveLanguageCode() {
        SupportedLanguage supported = requestLocaleResolver.resolveLanguage();
        if (supported == null) {
            return LanguageCode.UZ;
        }
        try {
            return LanguageCode.valueOf(supported.name());
        } catch (IllegalArgumentException ignored) {
            return LanguageCode.UZ;
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }
}
