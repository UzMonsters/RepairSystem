package com.example.darks.repair_auto.notification.application;

import com.example.darks.repair_auto.notification.domain.NotificationOutbox;
import com.example.darks.repair_auto.notification.infrastructure.persistence.NotificationOutboxRepository;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class NotificationOutboxService {

    private final NotificationOutboxRepository repository;
    private final NotificationRecipientResolver recipientResolver;
    private final NotificationTemplateService templateService;
    private final Clock clock;

    @Autowired
    public NotificationOutboxService(
            NotificationOutboxRepository repository,
            NotificationRecipientResolver recipientResolver,
            NotificationTemplateService templateService) {
        this(repository, recipientResolver, templateService, Clock.systemUTC());
    }

    NotificationOutboxService(
            NotificationOutboxRepository repository,
            NotificationRecipientResolver recipientResolver,
            NotificationTemplateService templateService,
            Clock clock) {
        this.repository = repository;
        this.recipientResolver = recipientResolver;
        this.templateService = templateService;
        this.clock = clock;
    }

    public void enqueue(NotificationEventFactory.NotificationEvent event) {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        LanguageCode language = recipientResolver.resolveLanguage(event.recipientType(), event.recipientId())
                .orElse(LanguageCode.UZ);
        var rendered = templateService.render(
                event.type(),
                event.recipientType(),
                event.payloadJson(),
                NotificationOutbox.PAYLOAD_VERSION,
                language);
        try {
            repository.saveAndFlush(new NotificationOutbox(
                    event.eventKey(),
                    event.type(),
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
            repository.findByEventKey(event.eventKey()).orElseThrow(() -> exception);
        }
    }
}
