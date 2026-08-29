package com.example.darks.repair_auto.realtime.event.application;

import com.example.darks.repair_auto.chat.application.ChatPushNotificationService;
import com.example.darks.repair_auto.chat.infrastructure.persistence.ChatMessageRepository;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.realtime.delivery.RealtimeEventPublisher;
import com.example.darks.repair_auto.realtime.event.RealtimeEvent;
import com.example.darks.repair_auto.realtime.event.RealtimeEventType;
import com.example.darks.repair_auto.realtime.event.dto.AssignmentEventPayload;
import com.example.darks.repair_auto.realtime.event.dto.AttachmentEventPayload;
import com.example.darks.repair_auto.realtime.event.dto.DashboardInvalidatedPayload;
import com.example.darks.repair_auto.realtime.event.dto.DiagnosisEventPayload;
import com.example.darks.repair_auto.realtime.event.dto.NotificationEventPayload;
import com.example.darks.repair_auto.realtime.event.dto.RequestDeletedPayload;
import com.example.darks.repair_auto.realtime.event.dto.RequestEventPayload;
import com.example.darks.repair_auto.realtime.event.dto.ScheduleEventPayload;
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
                null,
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
                event.technicianId(),
                null,
                null,
                null);
        RealtimeEvent<RequestEventPayload> rtEvent = RealtimeEvent.of(RealtimeEventType.REQUEST_UPDATED, payload);

        realtimeEventPublisher.publishToStaff(rtEvent);
        if (event.customerId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.CUSTOMER, event.customerId(), rtEvent);
        }
        if (event.technicianId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.TECHNICIAN, event.technicianId(), rtEvent);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRequestAssignmentCreated(RequestAssignmentCreatedDomainEvent event) {
        LOGGER.debug("Publishing realtime REQUEST_ASSIGNMENT_CREATED for requestId={}, technicianId={}",
                event.requestId(), event.technicianId());
        AssignmentEventPayload payload = new AssignmentEventPayload(
                event.requestId(),
                event.requestNumber(),
                event.assignmentId(),
                event.technicianId(),
                null,
                event.customerId(),
                "CREATED",
                "ASSIGNED");
        RealtimeEvent<AssignmentEventPayload> rtEvent = RealtimeEvent.of(
                RealtimeEventType.REQUEST_ASSIGNMENT_CREATED,
                payload);

        realtimeEventPublisher.publishToStaff(rtEvent);
        if (event.technicianId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.TECHNICIAN, event.technicianId(), rtEvent);
        }

        realtimeEventPublisher.publishToStaff(RealtimeEvent.of(
                RealtimeEventType.DASHBOARD_INVALIDATED,
                new DashboardInvalidatedPayload("REQUEST_ASSIGNMENT_CREATED")));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRequestAssignmentAccepted(RequestAssignmentAcceptedDomainEvent event) {
        LOGGER.debug("Publishing realtime REQUEST_ASSIGNMENT_ACCEPTED for requestId={}, technicianId={}",
                event.requestId(), event.technicianId());
        AssignmentEventPayload payload = new AssignmentEventPayload(
                event.requestId(),
                event.requestNumber(),
                event.assignmentId(),
                event.technicianId(),
                null,
                event.customerId(),
                "ACCEPTED",
                "ASSIGNED");
        RealtimeEvent<AssignmentEventPayload> rtEvent = RealtimeEvent.of(
                RealtimeEventType.REQUEST_ASSIGNMENT_ACCEPTED,
                payload);

        realtimeEventPublisher.publishToStaff(rtEvent);
        if (event.customerId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.CUSTOMER, event.customerId(), rtEvent);
        }
        if (event.technicianId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.TECHNICIAN, event.technicianId(), rtEvent);
        }

        realtimeEventPublisher.publishToStaff(RealtimeEvent.of(
                RealtimeEventType.DASHBOARD_INVALIDATED,
                new DashboardInvalidatedPayload("REQUEST_ASSIGNMENT_ACCEPTED")));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRequestAssigned(RequestAssignedDomainEvent event) {
        LOGGER.debug("Publishing realtime REQUEST_ASSIGNMENT_ACCEPTED (compat) for requestId={}, technicianId={}",
                event.requestId(), event.technicianId());
        AssignmentEventPayload payload = new AssignmentEventPayload(
                event.requestId(),
                event.requestNumber(),
                event.assignmentId(),
                event.technicianId(),
                null,
                event.customerId(),
                "ACCEPTED",
                "ASSIGNED");
        RealtimeEvent<AssignmentEventPayload> rtEvent = RealtimeEvent.of(
                RealtimeEventType.REQUEST_ASSIGNMENT_ACCEPTED,
                payload);

        realtimeEventPublisher.publishToStaff(rtEvent);
        if (event.customerId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.CUSTOMER, event.customerId(), rtEvent);
        }
        if (event.technicianId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.TECHNICIAN, event.technicianId(), rtEvent);
        }

        realtimeEventPublisher.publishToStaff(RealtimeEvent.of(
                RealtimeEventType.DASHBOARD_INVALIDATED,
                new DashboardInvalidatedPayload("REQUEST_ASSIGNMENT_ACCEPTED")));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRequestAssignmentRejected(RequestAssignmentRejectedDomainEvent event) {
        LOGGER.debug("Publishing realtime REQUEST_ASSIGNMENT_REJECTED for requestId={}, technicianId={}",
                event.requestId(), event.technicianId());
        AssignmentEventPayload payload = new AssignmentEventPayload(
                event.requestId(),
                event.requestNumber(),
                event.assignmentId(),
                event.technicianId(),
                null,
                event.customerId(),
                "REJECTED",
                "NEW");
        RealtimeEvent<AssignmentEventPayload> rtEvent = RealtimeEvent.of(
                RealtimeEventType.REQUEST_ASSIGNMENT_REJECTED,
                payload);

        realtimeEventPublisher.publishToStaff(rtEvent);
        if (event.technicianId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.TECHNICIAN, event.technicianId(), rtEvent);
        }
        if (event.customerId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.CUSTOMER, event.customerId(), rtEvent);
        }

        realtimeEventPublisher.publishToStaff(RealtimeEvent.of(
                RealtimeEventType.DASHBOARD_INVALIDATED,
                new DashboardInvalidatedPayload("REQUEST_ASSIGNMENT_REJECTED")));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRequestReassigned(RequestReassignedDomainEvent event) {
        LOGGER.debug("Publishing realtime REQUEST_REASSIGNED for requestId={}, oldTech={}, newTech={}",
                event.requestId(), event.oldTechnicianId(), event.newTechnicianId());
        AssignmentEventPayload payload = new AssignmentEventPayload(
                event.requestId(),
                event.requestNumber(),
                event.assignmentId(),
                event.newTechnicianId(),
                event.oldTechnicianId(),
                event.customerId(),
                "REASSIGNED",
                "ASSIGNED");
        RealtimeEvent<AssignmentEventPayload> rtEvent = RealtimeEvent.of(
                RealtimeEventType.REQUEST_REASSIGNED,
                payload);

        realtimeEventPublisher.publishToStaff(rtEvent);
        if (event.oldTechnicianId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.TECHNICIAN, event.oldTechnicianId(), rtEvent);
        }
        if (event.newTechnicianId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.TECHNICIAN, event.newTechnicianId(), rtEvent);
        }
        if (event.customerId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.CUSTOMER, event.customerId(), rtEvent);
        }

        realtimeEventPublisher.publishToStaff(RealtimeEvent.of(
                RealtimeEventType.DASHBOARD_INVALIDATED,
                new DashboardInvalidatedPayload("REQUEST_REASSIGNED")));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRequestUnassigned(RequestUnassignedDomainEvent event) {
        LOGGER.debug("Publishing realtime REQUEST_UNASSIGNED for requestId={}, technicianId={}",
                event.requestId(), event.technicianId());
        AssignmentEventPayload payload = new AssignmentEventPayload(
                event.requestId(),
                event.requestNumber(),
                event.assignmentId(),
                event.technicianId(),
                null,
                event.customerId(),
                "UNASSIGNED",
                "NEW");
        RealtimeEvent<AssignmentEventPayload> rtEvent = RealtimeEvent.of(RealtimeEventType.REQUEST_UNASSIGNED, payload);

        realtimeEventPublisher.publishToStaff(rtEvent);
        if (event.technicianId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.TECHNICIAN, event.technicianId(), rtEvent);
        }
        if (event.customerId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.CUSTOMER, event.customerId(), rtEvent);
        }

        realtimeEventPublisher.publishToStaff(RealtimeEvent.of(
                RealtimeEventType.DASHBOARD_INVALIDATED,
                new DashboardInvalidatedPayload("REQUEST_UNASSIGNED")));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRequestScheduleChanged(RequestScheduleChangedDomainEvent event) {
        LOGGER.debug("Publishing realtime REQUEST_SCHEDULE_CHANGED for requestId={}, action={}",
                event.requestId(), event.scheduleAction());
        ScheduleEventPayload payload = new ScheduleEventPayload(
                event.requestId(),
                event.requestNumber(),
                event.assignmentId(),
                event.technicianId(),
                event.customerId(),
                event.scheduledStart() != null ? event.scheduledStart().toInstant() : null,
                event.scheduledEnd() != null ? event.scheduledEnd().toInstant() : null,
                event.scheduleAction());
        RealtimeEvent<ScheduleEventPayload> rtEvent = RealtimeEvent.of(
                RealtimeEventType.REQUEST_SCHEDULE_CHANGED,
                payload);

        realtimeEventPublisher.publishToStaff(rtEvent);
        if (event.customerId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.CUSTOMER, event.customerId(), rtEvent);
        }
        if (event.technicianId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.TECHNICIAN, event.technicianId(), rtEvent);
        }

        realtimeEventPublisher.publishToStaff(RealtimeEvent.of(
                RealtimeEventType.DASHBOARD_INVALIDATED,
                new DashboardInvalidatedPayload("REQUEST_SCHEDULE_CHANGED")));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRequestDiagnosisUpdated(RequestDiagnosisUpdatedDomainEvent event) {
        LOGGER.debug("Publishing realtime REQUEST_DIAGNOSIS_UPDATED for requestId={}", event.requestId());
        DiagnosisEventPayload payload = new DiagnosisEventPayload(
                event.requestId(),
                event.requestNumber(),
                event.executionId(),
                event.technicianId(),
                event.customerId());
        RealtimeEvent<DiagnosisEventPayload> rtEvent = RealtimeEvent.of(
                RealtimeEventType.REQUEST_DIAGNOSIS_UPDATED,
                payload);

        realtimeEventPublisher.publishToStaff(rtEvent);
        if (event.technicianId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.TECHNICIAN, event.technicianId(), rtEvent);
        }
        if (event.customerId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.CUSTOMER, event.customerId(), rtEvent);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRequestAttachmentsChanged(RequestAttachmentsChangedDomainEvent event) {
        LOGGER.debug("Publishing realtime REQUEST_ATTACHMENTS_CHANGED for requestId={}, attachmentId={}, type={}",
                event.requestId(), event.attachmentId(), event.changeType());
        AttachmentEventPayload payload = new AttachmentEventPayload(
                event.requestId(),
                event.requestNumber(),
                event.attachmentId(),
                event.changeType(),
                event.customerId(),
                event.technicianId());
        RealtimeEvent<AttachmentEventPayload> rtEvent = RealtimeEvent.of(
                RealtimeEventType.REQUEST_ATTACHMENTS_CHANGED,
                payload);

        realtimeEventPublisher.publishToStaff(rtEvent);
        if (event.customerId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.CUSTOMER, event.customerId(), rtEvent);
        }
        if (event.technicianId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.TECHNICIAN, event.technicianId(), rtEvent);
        }
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
                event.fromStatus() != null ? event.fromStatus().name() : null,
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
    public void handleRequestDeleted(RequestDeletedDomainEvent event) {
        LOGGER.debug("Publishing realtime REQUEST_DELETED for requestId={}", event.requestId());
        RequestDeletedPayload payload = new RequestDeletedPayload(
                event.requestId(),
                event.requestNumber(),
                event.customerId(),
                event.technicianId());
        RealtimeEvent<RequestDeletedPayload> rtEvent = RealtimeEvent.of(RealtimeEventType.REQUEST_DELETED, payload);

        realtimeEventPublisher.publishToStaff(rtEvent);
        if (event.customerId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.CUSTOMER, event.customerId(), rtEvent);
        }
        if (event.technicianId() != null) {
            realtimeEventPublisher.publishToUser(ActorType.TECHNICIAN, event.technicianId(), rtEvent);
        }

        realtimeEventPublisher.publishToStaff(RealtimeEvent.of(
                RealtimeEventType.DASHBOARD_INVALIDATED,
                new DashboardInvalidatedPayload("REQUEST_DELETED")));
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
