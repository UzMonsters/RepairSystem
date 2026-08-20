package com.example.darks.repair_auto.realtime.delivery;

import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.realtime.event.RealtimeEvent;
import com.example.darks.repair_auto.realtime.event.RealtimeEventType;
import com.example.darks.repair_auto.realtime.session.RealtimeSessionRegistry;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class SpringWebSocketRealtimeEventPublisher implements RealtimeEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringWebSocketRealtimeEventPublisher.class);
    private static final String USER_QUEUE_EVENTS = "/queue/events";
    private static final String USER_QUEUE_CHAT = "/queue/chat";
    private static final String STAFF_TOPIC_EVENTS = "/topic/staff.events";

    private final SimpMessagingTemplate messagingTemplate;
    private final RealtimeSessionRegistry sessionRegistry;

    public SpringWebSocketRealtimeEventPublisher(
            SimpMessagingTemplate messagingTemplate,
            RealtimeSessionRegistry sessionRegistry) {
        this.messagingTemplate = messagingTemplate;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void publishToUser(ActorType actorType, Long actorId, RealtimeEvent<?> event) {
        if (actorType == null || actorId == null || event == null) {
            return;
        }

        Set<String> principalNames = sessionRegistry.findPrincipalNamesForActor(actorType, actorId);
        LOGGER.debug("Publishing realtime event {} to {} id={}, matching active sessions count={}",
                event.type(), actorType, actorId, principalNames.size());

        boolean isChatEvent = isChatEventType(event.type());

        if (principalNames.isEmpty()) {
            // If session registry didn't catch or user uses standard username pattern
            String fallbackPrincipal = actorType.name().toLowerCase() + ":" + actorId;
            sendToUserDestination(fallbackPrincipal, isChatEvent, event);
        } else {
            for (String principal : principalNames) {
                sendToUserDestination(principal, isChatEvent, event);
            }
        }
    }

    @Override
    public void publishToStaff(RealtimeEvent<?> event) {
        if (event == null) {
            return;
        }

        Set<String> staffPrincipals = sessionRegistry.findStaffPrincipalNames();
        LOGGER.debug("Publishing realtime event {} to staff, active staff sessions count={}",
                event.type(), staffPrincipals.size());

        boolean isChatEvent = isChatEventType(event.type());
        for (String principal : staffPrincipals) {
            sendToUserDestination(principal, isChatEvent, event);
        }

        // Also publish to staff broadcast topic for web dashboards/lists
        try {
            messagingTemplate.convertAndSend(STAFF_TOPIC_EVENTS, event);
        } catch (Exception ex) {
            LOGGER.warn("Failed to publish event to {}: {}", STAFF_TOPIC_EVENTS, ex.getMessage());
        }
    }

    @Override
    public void publishToRole(UserRole role, RealtimeEvent<?> event) {
        if (role == null || event == null) {
            return;
        }

        Set<String> rolePrincipals = sessionRegistry.findRolePrincipalNames(role);
        boolean isChatEvent = isChatEventType(event.type());
        for (String principal : rolePrincipals) {
            sendToUserDestination(principal, isChatEvent, event);
        }
    }

    @Override
    public void publishToAllAuthenticated(RealtimeEvent<?> event) {
        publishToStaff(event);
    }

    private void sendToUserDestination(String principal, boolean isChatEvent, RealtimeEvent<?> event) {
        try {
            if (isChatEvent) {
                messagingTemplate.convertAndSendToUser(principal, USER_QUEUE_CHAT, event);
            } else {
                messagingTemplate.convertAndSendToUser(principal, USER_QUEUE_EVENTS, event);
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to send realtime event to user {}: {}", principal, ex.getMessage());
        }
    }

    private boolean isChatEventType(RealtimeEventType type) {
        return type == RealtimeEventType.CHAT_MESSAGE_CREATED
                || type == RealtimeEventType.CHAT_MESSAGE_READ
                || type == RealtimeEventType.CHAT_TYPING_STARTED
                || type == RealtimeEventType.CHAT_TYPING_STOPPED;
    }
}
