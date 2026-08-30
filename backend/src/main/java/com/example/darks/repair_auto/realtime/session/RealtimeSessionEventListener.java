package com.example.darks.repair_auto.realtime.session;

import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class RealtimeSessionEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeSessionEventListener.class);

    private final RealtimeSessionRegistry sessionRegistry;

    public RealtimeSessionEventListener(RealtimeSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId() != null
                ? accessor.getSessionId()
                : (String) event.getMessage().getHeaders().get("simpSessionId");
        Principal principal = event.getUser() != null ? event.getUser() : accessor.getUser();
        if (sessionId != null && principal != null) {
            sessionRegistry.register(sessionId, principal);
            LOGGER.debug("WebSocket session connected: sessionId={}, principal={}", sessionId, principal.getName());
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        if (sessionId != null) {
            sessionRegistry.unregister(sessionId);
            LOGGER.debug("WebSocket session disconnected: sessionId={}", sessionId);
        }
    }
}
