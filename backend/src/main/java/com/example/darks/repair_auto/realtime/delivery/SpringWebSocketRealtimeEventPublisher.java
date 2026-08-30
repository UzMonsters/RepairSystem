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
    private static final String QUEUE_EVENTS_USER_PREFIX = "/queue/events-user";
    private static final String QUEUE_CHAT_USER_PREFIX = "/queue/chat-user";

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

        Set<String> sessionIds = sessionRegistry.findSessionIdsForActor(actorType, actorId);
        LOGGER.debug("Publishing realtime event {} to {} id={}, matching active sessions count={}",
                event.type(), actorType, actorId, sessionIds.size());

        boolean isChatEvent = isChatEventType(event.type());
        String prefix = isChatEvent ? QUEUE_CHAT_USER_PREFIX : QUEUE_EVENTS_USER_PREFIX;

        if (sessionIds.isEmpty()) {
            LOGGER.debug("No active sessions found for {} id={}, skipping delivery", actorType, actorId);
        } else {
            for (String sessionId : sessionIds) {
                sendToSessionDestination(prefix + sessionId, event);
            }
        }
    }

    @Override
    public void publishToStaff(RealtimeEvent<?> event) {
        if (event == null) {
            return;
        }

        Set<String> staffSessions = sessionRegistry.findStaffSessionIds();
        LOGGER.debug("Publishing realtime event {} to staff, active staff sessions count={}",
                event.type(), staffSessions.size());

        boolean isChatEvent = isChatEventType(event.type());
        String prefix = isChatEvent ? QUEUE_CHAT_USER_PREFIX : QUEUE_EVENTS_USER_PREFIX;

        for (String sessionId : staffSessions) {
            sendToSessionDestination(prefix + sessionId, event);
        }
    }

    @Override
    public void publishToRole(UserRole role, RealtimeEvent<?> event) {
        if (role == null || event == null) {
            return;
        }

        Set<String> roleSessions = sessionRegistry.findRoleSessionIds(role);
        boolean isChatEvent = isChatEventType(event.type());
        String prefix = isChatEvent ? QUEUE_CHAT_USER_PREFIX : QUEUE_EVENTS_USER_PREFIX;

        for (String sessionId : roleSessions) {
            sendToSessionDestination(prefix + sessionId, event);
        }
    }

    @Override
    public void publishToAllAuthenticated(RealtimeEvent<?> event) {
        publishToStaff(event);
    }

    private void sendToSessionDestination(String destination, RealtimeEvent<?> event) {
        try {
            messagingTemplate.convertAndSend(destination, event);
        } catch (Exception ex) {
            LOGGER.warn("Failed to send realtime event to destination {}: {}", destination, ex.getMessage());
        }
    }

    private boolean isChatEventType(RealtimeEventType type) {
        return type == RealtimeEventType.CHAT_MESSAGE_CREATED
                || type == RealtimeEventType.CHAT_MESSAGE_READ
                || type == RealtimeEventType.CHAT_TYPING_STARTED
                || type == RealtimeEventType.CHAT_TYPING_STOPPED;
    }
}
