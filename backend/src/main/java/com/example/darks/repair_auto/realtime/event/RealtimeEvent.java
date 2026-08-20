package com.example.darks.repair_auto.realtime.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RealtimeEvent<T>(
        UUID eventId,
        RealtimeEventType type,
        Instant occurredAt,
        T payload
) {
    public RealtimeEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static <T> RealtimeEvent<T> of(RealtimeEventType type, T payload) {
        return new RealtimeEvent<>(UUID.randomUUID(), type, Instant.now(), payload);
    }

    public static <T> RealtimeEvent<T> of(UUID eventId, RealtimeEventType type, T payload) {
        return new RealtimeEvent<>(eventId, type, Instant.now(), payload);
    }
}
