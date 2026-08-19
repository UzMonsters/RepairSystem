package com.example.darks.repair_auto.notification.application;

import com.example.darks.repair_auto.notification.domain.NotificationChannel;
import com.example.darks.repair_auto.notification.domain.NotificationOutbox;
import com.example.darks.repair_auto.notification.inbox.application.UserNotificationService;
import com.example.darks.repair_auto.notification.infrastructure.persistence.NotificationOutboxRepository;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class NotificationOutboxService {

    private final NotificationOutboxRepository repository;
    private final NotificationRecipientResolver recipientResolver;
    private final NotificationTemplateService templateService;
    private final NotificationChannelResolver channelResolver;
    private final UserNotificationService userNotificationService;
    private final Clock clock;

    @Autowired
    public NotificationOutboxService(
            NotificationOutboxRepository repository,
            NotificationRecipientResolver recipientResolver,
            NotificationTemplateService templateService,
            NotificationChannelResolver channelResolver,
            UserNotificationService userNotificationService) {
        this(repository, recipientResolver, templateService, channelResolver, userNotificationService, Clock.systemUTC());
    }

    NotificationOutboxService(
            NotificationOutboxRepository repository,
            NotificationRecipientResolver recipientResolver,
            NotificationTemplateService templateService,
            NotificationChannelResolver channelResolver,
            UserNotificationService userNotificationService,
            Clock clock) {
        this.repository = repository;
        this.recipientResolver = recipientResolver;
        this.templateService = templateService;
        this.channelResolver = channelResolver;
        this.userNotificationService = userNotificationService;
        this.clock = clock;
    }

    public void enqueue(NotificationEventFactory.NotificationEvent event) {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);

        if (userNotificationService != null) {
            userNotificationService.recordFromEvent(event);
        }

        LanguageCode language = recipientResolver.resolveLanguage(event.recipientType(), event.recipientId())
                .orElse(LanguageCode.UZ);
        var rendered = templateService.render(
                event.type(),
                event.recipientType(),
                event.payloadJson(),
                NotificationOutbox.PAYLOAD_VERSION,
                language);

        Set<NotificationChannel> channels = channelResolver.resolve(
                event.type(),
                event.recipientType(),
                event.recipientId());

        for (NotificationChannel channel : channels) {
            String channelEventKey = event.eventKey() + ":" + channel.name().toLowerCase();
            try {
                repository.saveAndFlush(new NotificationOutbox(
                        channelEventKey,
                        event.type(),
                        channel,
                        event.recipientType(),
                        event.recipientId(),
                        event.repairRequest(),
                        event.templateKey(),
                        event.payloadJson(),
                        rendered.language().name(),
                        rendered.title(),
                        rendered.message(),
                        now));
            } catch (DataIntegrityViolationException exception) {
                repository.findByEventKey(channelEventKey).orElseThrow(() -> exception);
            }
        }
    }
}
