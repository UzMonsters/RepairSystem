package com.example.darks.repair_auto.notification.infrastructure.worker;

import org.springframework.stereotype.Component;

@Component("notificationScheduleDelay")
public class NotificationScheduleDelay {

    private final NotificationProperties properties;

    public NotificationScheduleDelay(NotificationProperties properties) {
        this.properties = properties;
    }

    public long getValue() {
        return properties.getPollInterval().toMillis();
    }
}
