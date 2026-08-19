package com.example.darks.repair_auto.notification.push.gateway;

import com.google.firebase.messaging.Message;

public interface FirebaseMessagingClient {

    String send(Message message) throws Exception;
}
