package com.example.darks.repair_auto.chat.application;

import com.example.darks.repair_auto.identity.domain.ActorType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class TypingThrottleService {

    private static final long THROTTLE_INTERVAL_MILLIS = 2000; // 2 seconds

    private final Map<String, Long> lastTypingTimestamps = new ConcurrentHashMap<>();

    public boolean canSendTyping(Long conversationId, ActorType actorType, Long actorId) {
        if (conversationId == null || actorType == null || actorId == null) {
            return false;
        }

        String key = conversationId + ":" + actorType.name() + ":" + actorId;
        long now = System.currentTimeMillis();

        Long last = lastTypingTimestamps.get(key);
        if (last != null && (now - last) < THROTTLE_INTERVAL_MILLIS) {
            return false;
        }

        lastTypingTimestamps.put(key, now);
        return true;
    }
}
