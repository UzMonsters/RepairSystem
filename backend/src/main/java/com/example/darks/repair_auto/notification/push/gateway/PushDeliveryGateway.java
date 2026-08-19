package com.example.darks.repair_auto.notification.push.gateway;

public interface PushDeliveryGateway {

    PushDeliveryResult deliver(PushDeliveryCommand command);
}
