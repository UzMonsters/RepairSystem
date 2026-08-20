package com.example.darks.repair_auto.realtime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.realtime.delivery.RealtimeEventPublisher;
import com.example.darks.repair_auto.realtime.event.application.ChatMessageCreatedDomainEvent;
import com.example.darks.repair_auto.realtime.event.application.ParticipantRecipient;
import com.example.darks.repair_auto.realtime.event.application.RealtimeDomainEventListener;
import com.example.darks.repair_auto.realtime.event.application.RequestCreatedDomainEvent;
import com.example.darks.repair_auto.realtime.event.application.RequestStatusChangedDomainEvent;
import com.example.darks.repair_auto.realtime.event.dto.ChatMessagePayload;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class RealtimeDomainEventIntegrationTest extends PostgreSqlIntegrationTest {

    @MockitoSpyBean
    private RealtimeEventPublisher realtimeEventPublisher;

    @Autowired
    private RealtimeDomainEventListener realtimeDomainEventListener;

    @Test
    void onChatMessageCreated_publishesToParticipants() {
        ChatMessagePayload payload = new ChatMessagePayload(
                10L,
                1L,
                "CUSTOMER",
                100L,
                "client-id-1",
                "TEXT",
                "Hello!",
                null,
                null,
                Instant.now());

        ChatMessageCreatedDomainEvent event = new ChatMessageCreatedDomainEvent(
                1L,
                10L,
                ActorType.CUSTOMER,
                100L,
                List.of(
                        new ParticipantRecipient(ActorType.TECHNICIAN, 200L)),
                payload);

        realtimeDomainEventListener.handleChatMessageCreated(event);

        verify(realtimeEventPublisher).publishToUser(eq(ActorType.CUSTOMER), eq(100L), any());
        verify(realtimeEventPublisher).publishToUser(eq(ActorType.TECHNICIAN), eq(200L), any());
    }

    @Test
    void onChatMessageCreated_whenRecipientsContainSender_publishesToSenderOnlyOnce() {
        ChatMessagePayload payload = new ChatMessagePayload(
                11L,
                1L,
                "CUSTOMER",
                100L,
                "client-id-2",
                "TEXT",
                "Hello again!",
                null,
                null,
                Instant.now());

        ChatMessageCreatedDomainEvent event = new ChatMessageCreatedDomainEvent(
                1L,
                11L,
                ActorType.CUSTOMER,
                100L,
                List.of(
                        new ParticipantRecipient(ActorType.CUSTOMER, 100L),
                        new ParticipantRecipient(ActorType.TECHNICIAN, 200L)),
                payload);

        realtimeDomainEventListener.handleChatMessageCreated(event);

        verify(realtimeEventPublisher, times(1)).publishToUser(eq(ActorType.CUSTOMER), eq(100L), any());
        verify(realtimeEventPublisher).publishToUser(eq(ActorType.TECHNICIAN), eq(200L), any());
    }

    @Test
    void onRequestCreated_publishesToStaffAndCustomer() {
        RequestCreatedDomainEvent event = new RequestCreatedDomainEvent(
                55L,
                "REQ-55",
                101L);

        realtimeDomainEventListener.handleRequestCreated(event);

        verify(realtimeEventPublisher, atLeastOnce()).publishToStaff(any());
        verify(realtimeEventPublisher).publishToUser(eq(ActorType.CUSTOMER), eq(101L), any());
    }

    @Test
    void onRequestStatusChanged_publishesToStaffCustomerTechnician() {
        RequestStatusChangedDomainEvent event = new RequestStatusChangedDomainEvent(
                77L,
                "REQ-77",
                102L,
                202L,
                RepairRequestStatus.ASSIGNED,
                RepairRequestStatus.IN_PROGRESS);

        realtimeDomainEventListener.handleRequestStatusChanged(event);

        verify(realtimeEventPublisher, atLeastOnce()).publishToStaff(any());
        verify(realtimeEventPublisher).publishToUser(eq(ActorType.CUSTOMER), eq(102L), any());
        verify(realtimeEventPublisher).publishToUser(eq(ActorType.TECHNICIAN), eq(202L), any());
    }
}
