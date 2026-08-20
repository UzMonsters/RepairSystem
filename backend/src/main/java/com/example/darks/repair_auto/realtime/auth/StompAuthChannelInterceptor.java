package com.example.darks.repair_auto.realtime.auth;

import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import java.security.Principal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);

    private final WebSocketAuthenticator authenticator;

    public StompAuthChannelInterceptor(WebSocketAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            handleSubscribe(accessor);
        } else if (StompCommand.SEND.equals(accessor.getCommand())) {
            handleSend(accessor);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String authHeader = extractAuthHeader(accessor);
        if (authHeader == null || authHeader.isBlank()) {
            LOGGER.debug("STOMP CONNECT rejected: missing Authorization header");
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        try {
            Authentication authentication = authenticator.authenticate(authHeader);
            accessor.setUser(authentication);
            LOGGER.debug("STOMP CONNECT authenticated for principal: {}", authentication.getName());
        } catch (Exception ex) {
            LOGGER.debug("STOMP CONNECT rejected: authentication failed ({})", ex.getMessage());
            if (ex instanceof BusinessException be) {
                throw be;
            }
            throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        String destination = accessor.getDestination();
        if (destination == null || destination.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        // Allow user-specific queues (/user/queue/... or /user/...)
        if (destination.startsWith("/user/queue/") || destination.startsWith("/user/")) {
            return;
        }

        // Allow staff topic only for staff actors
        if (destination.startsWith("/topic/staff")) {
            if (!isStaffUser(user)) {
                throw new AccessDeniedException("Destination forbidden for non-staff user");
            }
            return;
        }

        // Prohibit subscribing to arbitrary broadcast destinations
        if (destination.startsWith("/topic/") || destination.startsWith("/queue/")) {
            // Direct subscription to /queue/ without /user/ is not permitted for client-specific data
            throw new AccessDeniedException("Direct subscription to " + destination + " is not permitted");
        }
    }

    private void handleSend(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith("/app/")) {
            throw new AccessDeniedException("Clients may only send messages to /app destinations");
        }
    }

    private String extractAuthHeader(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            return authHeaders.get(0);
        }
        List<String> lowerAuthHeaders = accessor.getNativeHeader("authorization");
        if (lowerAuthHeaders != null && !lowerAuthHeaders.isEmpty()) {
            return lowerAuthHeaders.get(0);
        }
        String passcode = accessor.getPasscode();
        if (passcode != null && !passcode.isBlank()) {
            return passcode;
        }
        return null;
    }

    private boolean isStaffUser(Principal principal) {
        if (principal instanceof Authentication authentication) {
            Object p = authentication.getPrincipal();
            return p instanceof AuthenticatedUser;
        }
        return principal instanceof AuthenticatedUser;
    }
}
