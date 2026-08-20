package com.example.darks.repair_auto.chat.api;

import com.example.darks.repair_auto.chat.api.dto.ChatMessageResponse;
import com.example.darks.repair_auto.chat.api.dto.ConversationDetailResponse;
import com.example.darks.repair_auto.chat.api.dto.ConversationSummaryResponse;
import com.example.darks.repair_auto.chat.api.dto.MarkReadRequest;
import com.example.darks.repair_auto.chat.api.dto.SendMessageRequest;
import com.example.darks.repair_auto.chat.application.ChatService;
import com.example.darks.repair_auto.chat.domain.Conversation;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.infrastructure.security.CurrentActorResolver;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mobile/me/conversations")
public class MobileChatController {

    private final ChatService chatService;
    private final CurrentActorResolver currentActorResolver;

    public MobileChatController(
            ChatService chatService,
            CurrentActorResolver currentActorResolver) {
        this.chatService = chatService;
        this.currentActorResolver = currentActorResolver;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ConversationSummaryResponse>> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        AuthenticatedMobileActor actor = currentActorResolver.requireMobileActor();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "updatedAt"));
        return ResponseEntity.ok(chatService.listConversationsForActor(actor.actorType(), actor.actorId(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationDetailResponse> get(@PathVariable("id") Long id) {
        AuthenticatedMobileActor actor = currentActorResolver.requireMobileActor();
        return ResponseEntity.ok(chatService.getConversationDetails(id, actor.actorType(), actor.actorId()));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<PageResponse<ChatMessageResponse>> messages(
            @PathVariable("id") Long id,
            @RequestParam(name = "beforeId", required = false) Long beforeId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        AuthenticatedMobileActor actor = currentActorResolver.requireMobileActor();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(chatService.getMessageHistory(id, beforeId, pageable, actor.actorType(), actor.actorId()));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable("id") Long id,
            @RequestBody SendMessageRequest request) {
        AuthenticatedMobileActor actor = currentActorResolver.requireMobileActor();
        SendMessageRequest safeRequest = new SendMessageRequest(
                id,
                request.clientMessageId(),
                request.type(),
                request.text(),
                request.attachmentId(),
                request.replyToMessageId());
        return ResponseEntity.ok(chatService.sendMessage(safeRequest, actor.actorType(), actor.actorId()));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable("id") Long id,
            @RequestBody MarkReadRequest request) {
        AuthenticatedMobileActor actor = currentActorResolver.requireMobileActor();
        chatService.markAsRead(id, request.messageId(), actor.actorType(), actor.actorId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/{requestId}")
    public ResponseEntity<ConversationDetailResponse> getOrCreateForRequest(
            @PathVariable("requestId") Long requestId) {
        AuthenticatedMobileActor actor = currentActorResolver.requireMobileActor();
        Conversation conv = chatService.getOrCreateCustomerTechnicianConversationForMobileActor(
                requestId,
                actor.actorType(),
                actor.actorId());
        return ResponseEntity.ok(chatService.getConversationDetails(conv.getId(), actor.actorType(), actor.actorId()));
    }
}
