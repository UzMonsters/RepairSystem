package com.example.darks.repair_auto.chat.api;

import com.example.darks.repair_auto.chat.api.dto.ChatMessageResponse;
import com.example.darks.repair_auto.chat.api.dto.ConversationDetailResponse;
import com.example.darks.repair_auto.chat.api.dto.ConversationSummaryResponse;
import com.example.darks.repair_auto.chat.api.dto.MarkReadRequest;
import com.example.darks.repair_auto.chat.api.dto.SendMessageRequest;
import com.example.darks.repair_auto.chat.application.ChatService;
import com.example.darks.repair_auto.chat.domain.Conversation;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
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
@RequestMapping("/api/v1/conversations")
public class StaffChatController {

    private final ChatService chatService;
    private final CurrentActorResolver currentActorResolver;

    public StaffChatController(
            ChatService chatService,
            CurrentActorResolver currentActorResolver) {
        this.chatService = chatService;
        this.currentActorResolver = currentActorResolver;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ConversationSummaryResponse>> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        AuthenticatedUser user = currentActorResolver.requireStaff();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "updatedAt"));
        return ResponseEntity.ok(chatService.listConversationsForActor(ActorType.STAFF, user.id(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationDetailResponse> get(@PathVariable("id") Long id) {
        AuthenticatedUser user = currentActorResolver.requireStaff();
        return ResponseEntity.ok(chatService.getConversationDetails(id, ActorType.STAFF, user.id()));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<PageResponse<ChatMessageResponse>> messages(
            @PathVariable("id") Long id,
            @RequestParam(name = "beforeId", required = false) Long beforeId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        AuthenticatedUser user = currentActorResolver.requireStaff();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(chatService.getMessageHistory(id, beforeId, pageable, ActorType.STAFF, user.id()));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable("id") Long id,
            @RequestBody SendMessageRequest request) {
        AuthenticatedUser user = currentActorResolver.requireStaff();
        SendMessageRequest safeRequest = new SendMessageRequest(
                id,
                request.clientMessageId(),
                request.type(),
                request.text(),
                request.attachmentId(),
                request.replyToMessageId());
        return ResponseEntity.ok(chatService.sendMessage(safeRequest, ActorType.STAFF, user.id()));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable("id") Long id,
            @RequestBody MarkReadRequest request) {
        AuthenticatedUser user = currentActorResolver.requireStaff();
        chatService.markAsRead(id, request.messageId(), ActorType.STAFF, user.id());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/{requestId}/technician-manager")
    public ResponseEntity<ConversationDetailResponse> getOrCreateTechnicianManager(
            @PathVariable("requestId") Long requestId) {
        AuthenticatedUser user = currentActorResolver.requireStaff();
        Conversation conv = chatService.getOrCreateTechnicianManagerConversation(requestId, user.id());
        return ResponseEntity.ok(chatService.getConversationDetails(conv.getId(), ActorType.STAFF, user.id()));
    }

    @PostMapping("/requests/{requestId}/customer-technician")
    public ResponseEntity<ConversationDetailResponse> getOrCreateCustomerTechnician(
            @PathVariable("requestId") Long requestId) {
        AuthenticatedUser user = currentActorResolver.requireStaff();
        Conversation conv = chatService.getOrCreateCustomerTechnicianConversation(requestId);
        return ResponseEntity.ok(chatService.getConversationDetails(conv.getId(), ActorType.STAFF, user.id()));
    }
}
