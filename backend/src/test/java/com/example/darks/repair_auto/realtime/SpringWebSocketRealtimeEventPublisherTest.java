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
        when(sessionRegistry.findSessionIdsForActor(ActorType.CUSTOMER, 100L))
                .thenReturn(Set.of("session-100"));

        RealtimeEvent<String> chatEvent = RealtimeEvent.of(
                RealtimeEventType.CHAT_MESSAGE_CREATED,
                "chat-payload");

        publisher.publishToUser(ActorType.CUSTOMER, 100L, chatEvent);

        verify(messagingTemplate).convertAndSend(eq("/queue/chat-user" + "session-100"), eq(chatEvent));
        verify(messagingTemplate, never()).convertAndSend(eq("/queue/events-user" + "session-100"), any(Object.class));
    }

    @Test
    void publishToUser_domainEvent_publishesExclusivelyToEventsQueue() {
        when(sessionRegistry.findSessionIdsForActor(ActorType.TECHNICIAN, 200L))
                .thenReturn(Set.of("session-200"));

        RealtimeEvent<String> requestEvent = RealtimeEvent.of(
                RealtimeEventType.REQUEST_STATUS_CHANGED,
                "request-payload");

        publisher.publishToUser(ActorType.TECHNICIAN, 200L, requestEvent);

        verify(messagingTemplate).convertAndSend(eq("/queue/events-user" + "session-200"), eq(requestEvent));
        verify(messagingTemplate, never()).convertAndSend(eq("/queue/chat-user" + "session-200"), any(Object.class));
    }

    @Test
    void publishToStaff_domainEvent_publishesExclusivelyToEventsQueueAndNeverToBroadcastTopic() {
        when(sessionRegistry.findStaffSessionIds())
                .thenReturn(Set.of("session-staff-1"));

        RealtimeEvent<String> dashboardEvent = RealtimeEvent.of(
                RealtimeEventType.DASHBOARD_INVALIDATED,
                "dash-payload");

        publisher.publishToStaff(dashboardEvent);

        verify(messagingTemplate).convertAndSend(eq("/queue/events-user" + "session-staff-1"), eq(dashboardEvent));
        verify(messagingTemplate, never()).convertAndSend(eq("/queue/chat-user" + "session-staff-1"), any(Object.class));
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/staff.events"), any(Object.class));
    }
}
