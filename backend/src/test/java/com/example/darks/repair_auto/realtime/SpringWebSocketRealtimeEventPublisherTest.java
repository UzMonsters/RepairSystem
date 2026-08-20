package com.example.darks.repair_auto.realtime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.realtime.delivery.SpringWebSocketRealtimeEventPublisher;
import com.example.darks.repair_auto.realtime.event.RealtimeEvent;
import com.example.darks.repair_auto.realtime.event.RealtimeEventType;
import com.example.darks.repair_auto.realtime.session.RealtimeSessionRegistry;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class SpringWebSocketRealtimeEventPublisherTest {

    private SimpMessagingTemplate messagingTemplate;
    private RealtimeSessionRegistry sessionRegistry;
    private SpringWebSocketRealtimeEventPublisher publisher;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        sessionRegistry = mock(RealtimeSessionRegistry.class);
        publisher = new SpringWebSocketRealtimeEventPublisher(messagingTemplate, sessionRegistry);
    }

    @Test
    void publishToUser_chatEvent_publishesExclusivelyToChatQueue() {
        when(sessionRegistry.findPrincipalNamesForActor(ActorType.CUSTOMER, 100L))
                .thenReturn(Set.of("customer:100"));

        RealtimeEvent<String> chatEvent = RealtimeEvent.of(
                RealtimeEventType.CHAT_MESSAGE_CREATED,
                "chat-payload");

        publisher.publishToUser(ActorType.CUSTOMER, 100L, chatEvent);

        verify(messagingTemplate).convertAndSendToUser(eq("customer:100"), eq("/queue/chat"), eq(chatEvent));
        verify(messagingTemplate, never()).convertAndSendToUser(eq("customer:100"), eq("/queue/events"), any());
    }

    @Test
    void publishToUser_domainEvent_publishesExclusivelyToEventsQueue() {
        when(sessionRegistry.findPrincipalNamesForActor(ActorType.TECHNICIAN, 200L))
                .thenReturn(Set.of("technician:200"));

        RealtimeEvent<String> requestEvent = RealtimeEvent.of(
                RealtimeEventType.REQUEST_STATUS_CHANGED,
                "request-payload");

        publisher.publishToUser(ActorType.TECHNICIAN, 200L, requestEvent);

        verify(messagingTemplate).convertAndSendToUser(eq("technician:200"), eq("/queue/events"), eq(requestEvent));
        verify(messagingTemplate, never()).convertAndSendToUser(eq("technician:200"), eq("/queue/chat"), any());
    }

    @Test
    void publishToStaff_domainEvent_publishesToEventsQueueAndStaffTopic() {
        when(sessionRegistry.findStaffPrincipalNames())
                .thenReturn(Set.of("staff:1"));

        RealtimeEvent<String> dashboardEvent = RealtimeEvent.of(
                RealtimeEventType.DASHBOARD_INVALIDATED,
                "dash-payload");

        publisher.publishToStaff(dashboardEvent);

        verify(messagingTemplate).convertAndSendToUser(eq("staff:1"), eq("/queue/events"), eq(dashboardEvent));
        verify(messagingTemplate, never()).convertAndSendToUser(eq("staff:1"), eq("/queue/chat"), any());
        verify(messagingTemplate).convertAndSend(eq("/topic/staff.events"), eq(dashboardEvent));
    }
}
