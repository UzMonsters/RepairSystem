package com.example.darks.repair_auto.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.notification.domain.NotificationChannel;
import com.example.darks.repair_auto.notification.domain.NotificationOutbox;
import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
import com.example.darks.repair_auto.notification.domain.NotificationType;
import com.example.darks.repair_auto.notification.inbox.application.UserNotificationService;
import com.example.darks.repair_auto.notification.infrastructure.persistence.NotificationOutboxRepository;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationOutboxServiceTest {

    private NotificationOutboxRepository repository;
    private NotificationRecipientResolver recipientResolver;
    private NotificationTemplateService templateService;
    private NotificationChannelResolver channelResolver;
    private UserNotificationService userNotificationService;
    private Clock clock;
    private NotificationOutboxService service;

    private RepairRequest repairRequest;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationOutboxRepository.class);
        recipientResolver = mock(NotificationRecipientResolver.class);
        templateService = mock(NotificationTemplateService.class);
        channelResolver = mock(NotificationChannelResolver.class);
        userNotificationService = mock(UserNotificationService.class);
        clock = Clock.fixed(Instant.parse("2026-08-18T10:00:00Z"), ZoneOffset.UTC);

        service = new NotificationOutboxService(
                repository,
                recipientResolver,
                templateService,
                channelResolver,
                userNotificationService,
                clock);

        repairRequest = mock(RepairRequest.class);
        when(repairRequest.getId()).thenReturn(101L);
        when(repairRequest.getRequestNumber()).thenReturn("REQ-2026-000042");
    }

    @Test
    void givenBusinessEventWithMultiChannels_whenEnqueue_thenSavesOutboxForBothTelegramAndPushAndRecordsInbox() {
        NotificationEventFactory.NotificationEvent event = new NotificationEventFactory.NotificationEvent(
                "req:101:status:COMPLETED:customer:42",
                NotificationType.REPAIR_COMPLETED,
                NotificationRecipientType.CUSTOMER,
                42L,
                repairRequest,
                "notification.repair.completed",
                "{\"requestNumber\":\"REQ-2026-000042\"}");

        when(recipientResolver.resolveLanguage(NotificationRecipientType.CUSTOMER, 42L))
                .thenReturn(Optional.of(LanguageCode.UZ));
        when(templateService.render(NotificationType.REPAIR_COMPLETED, NotificationRecipientType.CUSTOMER, event.payloadJson(), 1, LanguageCode.UZ))
                .thenReturn(new NotificationTemplateService.RenderedNotification(LanguageCode.UZ, "Ta'mirlash yakunlandi", "REQ-2026-000042 yakunlandi"));
        when(channelResolver.resolve(NotificationType.REPAIR_COMPLETED, NotificationRecipientType.CUSTOMER, 42L))
                .thenReturn(Set.of(NotificationChannel.TELEGRAM, NotificationChannel.PUSH));

        List<NotificationOutbox> saved = new ArrayList<>();
        when(repository.saveAndFlush(any(NotificationOutbox.class))).thenAnswer(invocation -> {
            NotificationOutbox outbox = invocation.getArgument(0);
            saved.add(outbox);
            return outbox;
        });

        service.enqueue(event);

        assertThat(saved).hasSize(2);
        assertThat(saved).extracting(NotificationOutbox::getChannel)
                .containsExactlyInAnyOrder(NotificationChannel.TELEGRAM, NotificationChannel.PUSH);
        assertThat(saved).extracting(NotificationOutbox::getEventKey)
                .containsExactlyInAnyOrder(
                        "req:101:status:COMPLETED:customer:42:telegram",
                        "req:101:status:COMPLETED:customer:42:push");

        verify(userNotificationService).recordFromEvent(event);
    }
}
