package com.example.darks.repair_auto.chat.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.darks.repair_auto.chat.api.dto.MarkReadRequest;
import com.example.darks.repair_auto.chat.api.dto.SendMessageRequest;
import com.example.darks.repair_auto.chat.api.dto.TypingRequest;
import com.example.darks.repair_auto.chat.application.ChatService;
import com.example.darks.repair_auto.chat.domain.ChatMessageType;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

class ChatStompControllerTest {

    private ChatService chatService;
    private ChatStompController controller;

    @BeforeEach
    void setUp() {
        chatService = mock(ChatService.class);
        controller = new ChatStompController(chatService);
    }

    @Test
    void sendMessage_withMobilePrincipalDelegatesWithActorIdentity() {
        var principal = new TestingAuthenticationToken(
                new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L),
                null);
        var request = new SendMessageRequest(
                100L,
                "client-message-id",
                ChatMessageType.TEXT,
                "hello",
                null,
                null);

        controller.sendMessage(request, principal);

        verify(chatService).sendMessage(eq(request), eq(ActorType.CUSTOMER), eq(42L));
    }

    @Test
    void markRead_withStaffPrincipalDelegatesWithStaffIdentity() {
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        org.mockito.Mockito.when(user.id()).thenReturn(7L);
        var principal = new TestingAuthenticationToken(user, null);
        var request = new MarkReadRequest(100L, 200L);

        controller.markRead(request, principal);

        verify(chatService).markAsRead(100L, 200L, ActorType.STAFF, 7L);
    }

    @Test
    void typing_withTechnicianPrincipalDelegatesWithActorIdentity() {
        var principal = new TestingAuthenticationToken(
                new AuthenticatedMobileActor(ActorType.TECHNICIAN, 88L),
                null);

        controller.typing(new TypingRequest(100L, true), principal);

        verify(chatService).handleTyping(100L, true, ActorType.TECHNICIAN, 88L);
    }

    @Test
    void sendMessage_withoutRecognizedPrincipalIsRejectedBeforeBusinessRules() {
        BusinessException exception = org.assertj.core.api.Assertions.catchThrowableOfType(
                () -> controller.sendMessage(
                        new SendMessageRequest(100L, "x", ChatMessageType.TEXT, "hello", null, null),
                        () -> "anonymous"),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTHENTICATION_REQUIRED);
        verify(chatService, never()).sendMessage(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void markRead_withoutPrincipalIsRejectedBeforeBusinessRules() {
        assertThatThrownBy(() -> controller.markRead(new MarkReadRequest(100L, 200L), null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTHENTICATION_REQUIRED);
        verify(chatService, never()).markAsRead(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }
}
