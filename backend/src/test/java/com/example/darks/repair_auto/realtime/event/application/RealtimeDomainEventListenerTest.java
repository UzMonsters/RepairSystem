package com.example.darks.repair_auto.realtime.event.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.darks.repair_auto.chat.application.ChatPushNotificationService;
import com.example.darks.repair_auto.chat.infrastructure.persistence.ChatMessageRepository;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.realtime.delivery.RealtimeEventPublisher;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RealtimeDomainEventListenerTest {

    private RealtimeEventPublisher realtimeEventPublisher;
    private ChatPushNotificationService chatPushNotificationService;
    private ChatMessageRepository chatMessageRepository;
    private RealtimeDomainEventListener listener;

    @BeforeEach
    void setUp() {
        realtimeEventPublisher = mock(RealtimeEventPublisher.class);
        chatPushNotificationService = mock(ChatPushNotificationService.class);
        chatMessageRepository = mock(ChatMessageRepository.class);

        listener = new RealtimeDomainEventListener(
                realtimeEventPublisher,
                chatPushNotificationService,
                chatMessageRepository);
    }

    @Test
    void givenAssignmentCreated_whenHandleRequestAssignmentCreated_thenPublishesToStaffAndTechnicianOnly() {
        RequestAssignmentCreatedDomainEvent event = new RequestAssignmentCreatedDomainEvent(
                100L,
                "REQ-2026-000100",
                300L,
                500L,
                200L);

        listener.handleRequestAssignmentCreated(event);

        verify(realtimeEventPublisher, times(2)).publishToStaff(any());
        verify(realtimeEventPublisher).publishToUser(eq(ActorType.TECHNICIAN), eq(300L), any());
        verify(realtimeEventPublisher, never()).publishToUser(eq(ActorType.CUSTOMER), any(), any());
    }

    @Test
    void givenAssignmentAccepted_whenHandleRequestAssignmentAccepted_thenPublishesToCustomerStaffAndTechnician() {
        RequestAssignmentAcceptedDomainEvent event = new RequestAssignmentAcceptedDomainEvent(
                100L,
                "REQ-2026-000100",
                200L,
                300L,
                500L);

        listener.handleRequestAssignmentAccepted(event);

        verify(realtimeEventPublisher, times(2)).publishToStaff(any());
        verify(realtimeEventPublisher).publishToUser(eq(ActorType.CUSTOMER), eq(200L), any());
        verify(realtimeEventPublisher).publishToUser(eq(ActorType.TECHNICIAN), eq(300L), any());
    }

    @Test
    void givenAssignmentRejected_whenHandleRequestAssignmentRejected_thenPublishesToStaffCustomerAndTechnician() {
        RequestAssignmentRejectedDomainEvent event = new RequestAssignmentRejectedDomainEvent(
                100L,
                "REQ-2026-000100",
                200L,
                300L,
                500L,
                "Too busy");

        listener.handleRequestAssignmentRejected(event);

        verify(realtimeEventPublisher, times(2)).publishToStaff(any());
        verify(realtimeEventPublisher).publishToUser(eq(ActorType.TECHNICIAN), eq(300L), any());
        verify(realtimeEventPublisher).publishToUser(eq(ActorType.CUSTOMER), eq(200L), any());
    }

    @Test
    void givenScheduleChanged_whenHandleRequestScheduleChanged_thenPublishesToStaffCustomerAndTechnician() {
        RequestScheduleChangedDomainEvent event = new RequestScheduleChangedDomainEvent(
                100L,
                "REQ-2026-000100",
                500L,
                300L,
                200L,
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(2),
                "SCHEDULED");

        listener.handleRequestScheduleChanged(event);

        verify(realtimeEventPublisher, times(2)).publishToStaff(any());
        verify(realtimeEventPublisher).publishToUser(eq(ActorType.CUSTOMER), eq(200L), any());
        verify(realtimeEventPublisher).publishToUser(eq(ActorType.TECHNICIAN), eq(300L), any());
    }

    @Test
    void givenDeleted_whenHandleRequestDeleted_thenPublishesToStaffCustomerAndTechnician() {
        RequestDeletedDomainEvent event = new RequestDeletedDomainEvent(
                100L,
                "REQ-2026-000100",
                200L,
                300L);

        listener.handleRequestDeleted(event);

        verify(realtimeEventPublisher, times(2)).publishToStaff(any());
        verify(realtimeEventPublisher).publishToUser(eq(ActorType.CUSTOMER), eq(200L), any());
        verify(realtimeEventPublisher).publishToUser(eq(ActorType.TECHNICIAN), eq(300L), any());
    }
}
