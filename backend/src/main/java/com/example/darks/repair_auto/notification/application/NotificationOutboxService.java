package com.example.darks.repair_auto.notification.application;

import com.example.darks.repair_auto.notification.domain.NotificationOutbox;
import com.example.darks.repair_auto.notification.infrastructure.persistence.NotificationOutboxRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class NotificationOutboxService {

    private final NotificationOutboxRepository repository;
    private final Clock clock;

    @Autowired
    public NotificationOutboxService(NotificationOutboxRepository repository) {
        this(repository, Clock.systemUTC());
    }

    NotificationOutboxService(NotificationOutboxRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void enqueue(NotificationEventFactory.NotificationEvent event) {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        try {
            repository.saveAndFlush(new NotificationOutbox(
                    event.eventKey(),
                    event.type(),
                    event.recipientType(),
                    event.recipientId(),
                    event.repairRequest(),
                    event.templateKey(),
                    event.payloadJson(),
                    now));
        } catch (DataIntegrityViolationException exception) {
            repository.findByEventKey(event.eventKey()).orElseThrow(() -> exception);
        }
    }
}
