package com.example.darks.repair_auto.realtime.delivery;

import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.realtime.event.RealtimeEvent;

public interface RealtimeEventPublisher {

    void publishToUser(ActorType actorType, Long actorId, RealtimeEvent<?> event);

    void publishToStaff(RealtimeEvent<?> event);

    void publishToRole(UserRole role, RealtimeEvent<?> event);

    void publishToAllAuthenticated(RealtimeEvent<?> event);
}
