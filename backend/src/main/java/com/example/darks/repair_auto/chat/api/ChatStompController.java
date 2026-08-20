package com.example.darks.repair_auto.chat.api;

import com.example.darks.repair_auto.chat.api.dto.MarkReadRequest;
import com.example.darks.repair_auto.chat.api.dto.SendMessageRequest;
import com.example.darks.repair_auto.chat.api.dto.TypingRequest;
import com.example.darks.repair_auto.chat.application.ChatService;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class ChatStompController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatStompController.class);

    private final ChatService chatService;

    public ChatStompController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageRequest request, Principal principal) {
        ActorInfo actor = resolveActor(principal);
        LOGGER.debug("STOMP /chat.send from {} id={}, conversationId={}",
                actor.actorType(), actor.actorId(), request.conversationId());
        chatService.sendMessage(request, actor.actorType(), actor.actorId());
    }

    @MessageMapping("/chat.read")
    public void markRead(@Payload MarkReadRequest request, Principal principal) {
        ActorInfo actor = resolveActor(principal);
        LOGGER.debug("STOMP /chat.read from {} id={}, conversationId={}, messageId={}",
                actor.actorType(), actor.actorId(), request.conversationId(), request.messageId());
        chatService.markAsRead(request.conversationId(), request.messageId(), actor.actorType(), actor.actorId());
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingRequest request, Principal principal) {
        ActorInfo actor = resolveActor(principal);
        chatService.handleTyping(
                request.conversationId(),
                Boolean.TRUE.equals(request.typing()),
                actor.actorType(),
                actor.actorId());
    }

    private record ActorInfo(ActorType actorType, Long actorId) {}

    private ActorInfo resolveActor(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        Object details = principal;
        if (principal instanceof Authentication auth) {
            details = auth.getPrincipal();
        }

        if (details instanceof AuthenticatedUser user) {
            return new ActorInfo(ActorType.STAFF, user.id());
        }
        if (details instanceof AuthenticatedMobileActor mobile) {
            return new ActorInfo(mobile.actorType(), mobile.actorId());
        }

        throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
    }
}
