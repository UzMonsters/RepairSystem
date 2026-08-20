package com.example.darks.repair_auto.realtime.event.application;

import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.chat.application.ChatPushNotificationService;
import com.example.darks.repair_auto.chat.infrastructure.persistence.ChatMessageRepository;
import com.example.darks.repair_auto.realtime.delivery.RealtimeEventPublisher;
import com.example.darks.repair_auto.realtime.event.RealtimeEvent;
import com.example.darks.repair_auto.realtime.event.RealtimeEventType;
import com.example.darks.repair_auto.realtime.event.dto.DashboardInvalidatedPayload;
import com.example.darks.repair_auto.realtime.event.dto.NotificationEventPayload;
import com.example.darks.repair_auto.realtime.event.dto.RequestEventPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RealtimeDomainEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeDomainEventListener.class);

    private final RealtimeEventPublisher realtimeEventPublisher;
    private final ChatPushNotificationService chatPushNotificationService;
    private final ChatMessageRepository chatMessageRepository;

    public RealtimeDomainEventListener(
            RealtimeEventPublisher realtimeEventPublisher,
            ChatPushNotificationService chatPushNotificationService,
            ChatMessageRepository chatMessageRepository) {
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.chatPushNotificationService = chatPushNotificationService;
        this.chatMessageRepository = chatMessageRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRequestCreated(RequestCreatedDomainEvent event) {
        LOGGER.debug("Publishing realtime REQUEST_CREATED for requestId={}", event.requestId());
        RequestEventPayload payload = new RequestEventPayload(
                event.requestId(),
                event.requestNumber(),
                event.customerId(),
                null,
                "NEW",
                null);
        RealtimeEvent<RequestEventPayload> rtEvent = RealtimeEvent.of(RealtimeEventType.REQUEST_CREATED, payload);

        realtimeEventPublisher.publishToStaff(rtEvent);
        if (event.customerId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.CUSTOMER, event.customerId(), rtEvent);
        }

        realtimeEventPublisher.publishToStaff(RealtimeEvent.of(
                RealtimeEventType.DASHBOARD_INVALIDATED,
                new DashboardInvalidatedPayload("REQUEST_CREATED")));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRequestUpdated(RequestUpdatedDomainEvent event) {
        LOGGER.debug("Publishing realtime REQUEST_UPDATED for requestId={}", event.requestId());
        RequestEventPayload payload = new RequestEventPayload(
                event.requestId(),
                event.requestNumber(),
                event.customerId(),
                null,
                null,
                null);
        RealtimeEvent<RequestEventPayload> rtEvent = RealtimeEvent.of(RealtimeEventType.REQUEST_UPDATED, payload);

        realtimeEventPublisher.publishToStaff(rtEvent);
        if (event.customerId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.CUSTOMER, event.customerId(), rtEvent);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRequestAssigned(RequestAssignedDomainEvent event) {
        LOGGER.debug("Publishing realtime REQUEST_ASSIGNED for requestId={}, technicianId={}",
                event.requestId(), event.technicianId());
        RequestEventPayload payload = new RequestEventPayload(
                event.requestId(),
                event.requestNumber(),
                event.customerId(),
                event.technicianId(),
                "ASSIGNED",
                null);
        RealtimeEvent<RequestEventPayload> rtEvent = RealtimeEvent.of(RealtimeEventType.REQUEST_ASSIGNED, payload);

        realtimeEventPublisher.publishToStaff(rtEvent);
        if (event.customerId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.CUSTOMER, event.customerId(), rtEvent);
        }
        if (event.technicianId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.TECHNICIAN, event.technicianId(), rtEvent);
        }

        realtimeEventPublisher.publishToStaff(RealtimeEvent.of(
                RealtimeEventType.DASHBOARD_INVALIDATED,
                new DashboardInvalidatedPayload("REQUEST_ASSIGNED")));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRequestUnassigned(RequestUnassignedDomainEvent event) {
        LOGGER.debug("Publishing realtime REQUEST_UNASSIGNED for requestId={}, technicianId={}",
                event.requestId(), event.technicianId());
        RequestEventPayload payload = new RequestEventPayload(
                event.requestId(),
                event.requestNumber(),
                event.customerId(),
                event.technicianId(),
                "NEW",
                null);
        RealtimeEvent<RequestEventPayload> rtEvent = RealtimeEvent.of(RealtimeEventType.REQUEST_UNASSIGNED, payload);

        realtimeEventPublisher.publishToStaff(rtEvent);
        if (event.customerId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.CUSTOMER, event.customerId(), rtEvent);
        }
        if (event.technicianId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.TECHNICIAN, event.technicianId(), rtEvent);
        }

        realtimeEventPublisher.publishToStaff(RealtimeEvent.of(
                RealtimeEventType.DASHBOARD_INVALIDATED,
                new DashboardInvalidatedPayload("REQUEST_UNASSIGNED")));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRequestStatusChanged(RequestStatusChangedDomainEvent event) {
        LOGGER.debug("Publishing realtime REQUEST_STATUS_CHANGED for requestId={}, status={}",
                event.requestId(), event.toStatus());
        RequestEventPayload payload = new RequestEventPayload(
                event.requestId(),
                event.requestNumber(),
                event.customerId(),
                event.technicianId(),
                event.toStatus() != null ? event.toStatus().name() : null,
                null);
        RealtimeEvent<RequestEventPayload> rtEvent = RealtimeEvent.of(RealtimeEventType.REQUEST_STATUS_CHANGED, payload);

        realtimeEventPublisher.publishToStaff(rtEvent);
        if (event.customerId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.CUSTOMER, event.customerId(), rtEvent);
        }
        if (event.technicianId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.TECHNICIAN, event.technicianId(), rtEvent);
        }

        realtimeEventPublisher.publishToStaff(RealtimeEvent.of(
                RealtimeEventType.DASHBOARD_INVALIDATED,
                new DashboardInvalidatedPayload("REQUEST_STATUS_CHANGED")));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreated(NotificationCreatedDomainEvent event) {
        NotificationEventPayload payload = new NotificationEventPayload(
                event.notificationId(),
                event.notificationType(),
                event.targetId(),
                event.target(),
                false);
        RealtimeEvent<NotificationEventPayload> rtEvent = RealtimeEvent.of(RealtimeEventType.NOTIFICATION_CREATED, payload);

        if (event.recipientType() == ActorType.STAFF) {
            realtimeEventPublisher.publishToUser(ActorType.STAFF, event.recipientId(), rtEvent);
        } else {
            realtimeEventPublisher.publishToUser(event.recipientType(), event.recipientId(), rtEvent);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationRead(NotificationReadDomainEvent event) {
        NotificationEventPayload payload = new NotificationEventPayload(
                event.notificationId(),
                null,
                null,
                null,
                true);
        RealtimeEvent<NotificationEventPayload> rtEvent = RealtimeEvent.of(RealtimeEventType.NOTIFICATION_READ, payload);

        realtimeEventPublisher.publishToUser(event.recipientType(), event.recipientId(), rtEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDashboardInvalidated(DashboardInvalidatedDomainEvent event) {
        realtimeEventPublisher.publishToStaff(RealtimeEvent.of(
                RealtimeEventType.DASHBOARD_INVALIDATED,
                new DashboardInvalidatedPayload(event.reason())));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatMessageCreated(ChatMessageCreatedDomainEvent event) {
        RealtimeEvent<?> rtEvent = RealtimeEvent.of(RealtimeEventType.CHAT_MESSAGE_CREATED, event.payload());

        // Publish to sender
        realtimeEventPublisher.publishToUser(event.senderType(), event.senderId(), rtEvent);

        // Publish to all active recipients
        for (ParticipantRecipient recipient : event.recipients()) {
            if (recipient.actorType() == event.senderType() && recipient.actorId().equals(event.senderId())) {
                continue;
            }
            if (recipient.actorType() == ActorType.STAFF) {
                realtimeEventPublisher.publishToUser(ActorType.STAFF, recipient.actorId(), rtEvent);
            } else {
                realtimeEventPublisher.publishToUser(recipient.actorType(), recipient.actorId(), rtEvent);
            }
        }

        chatMessageRepository.findByIdWithConversationAndRepairRequest(event.messageId())
                .ifPresent(message -> chatPushNotificationService.sendPushNotifications(
                        message.getConversation(),
                        message,
                        event.recipients()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatMessageRead(ChatMessageReadDomainEvent event) {
        RealtimeEvent<?> rtEvent = RealtimeEvent.of(RealtimeEventType.CHAT_MESSAGE_READ, event.payload());

        // Publish to all other recipients
        for (ParticipantRecipient recipient : event.recipients()) {
            if (recipient.actorType() == ActorType.STAFF) {
                realtimeEventPublisher.publishToUser(ActorType.STAFF, recipient.actorId(), rtEvent);
            } else {
                realtimeEventPublisher.publishToUser(recipient.actorType(), recipient.actorId(), rtEvent);
            }
        }
    }
}
