package com.example.darks.repair_auto.notification.push.gateway;

import com.example.darks.repair_auto.notification.push.domain.PushEndpoint;
import com.example.darks.repair_auto.notification.push.domain.PushPlatform;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FirebasePushDeliveryGateway implements PushDeliveryGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirebasePushDeliveryGateway.class);

    private final FirebaseMessagingClient messagingClient;

    public FirebasePushDeliveryGateway(FirebaseMessagingClient messagingClient) {
        this.messagingClient = messagingClient;
    }

    @Override
    public PushDeliveryResult deliver(PushDeliveryCommand command) {
        PushEndpoint endpoint = command.endpoint();
        try {
            Message message = buildMessage(command);
            LOGGER.debug("Sending FCM push message to endpoint id={}, ownerType={}, clientType={}, platform={}",
                    endpoint.getId(), endpoint.getOwnerType(), endpoint.getClientType(), endpoint.getPlatform());

            String messageId = messagingClient.send(message);
            LOGGER.info("Successfully delivered FCM push message for endpoint id={}, messageId={}",
                    endpoint.getId(), messageId);
            return PushDeliveryResult.success(messageId);
        } catch (FirebaseMessagingException e) {
            return classifyFirebaseException(endpoint, e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error during FCM push delivery for endpoint id={}: {}",
                    endpoint.getId(), e.getMessage());
            return PushDeliveryResult.retryableFailure("UNEXPECTED_ERROR", e.getMessage());
        }
    }

    public Message buildMessage(PushDeliveryCommand command) {
        PushEndpoint endpoint = command.endpoint();

        Message.Builder builder = Message.builder()
                .setToken(endpoint.getFcmRegistrationToken())
                .setNotification(Notification.builder()
                        .setTitle(command.title())
                        .setBody(command.body())
                        .build());

        Map<String, String> data = new HashMap<>();
        if (command.notificationId() != null) {
            data.put("notificationId", String.valueOf(command.notificationId()));
        }
        if (command.notificationType() != null) {
            data.put("notificationType", command.notificationType());
        }
        if (command.repairRequestId() != null) {
            data.put("repairRequestId", String.valueOf(command.repairRequestId()));
        }
        if (command.requestNumber() != null) {
            data.put("requestNumber", command.requestNumber());
        }
        if (command.route() != null) {
            data.put("route", command.route());
        }
        data.put("clientType", endpoint.getClientType().name());
        data.put("platform", endpoint.getPlatform().name());
        data.put("firebaseAppKey", endpoint.getFirebaseAppKey().name());

        if (command.additionalData() != null) {
            command.additionalData().forEach((k, v) -> {
                if (k != null && v != null) {
                    data.put(k, v);
                }
            });
        }
        builder.putAllData(data);

        applyPlatformConfig(builder, command);

        return builder.build();
    }

    private void applyPlatformConfig(Message.Builder builder, PushDeliveryCommand command) {
        PushPlatform platform = command.endpoint().getPlatform();
        if (platform == PushPlatform.WEB) {
            WebpushConfig webpushConfig = WebpushConfig.builder()
                    .putHeader("Urgency", "high")
                    .setNotification(WebpushNotification.builder()
                            .setTitle(command.title())
                            .setBody(command.body())
                            .setIcon("/icons/icon-192.png")
                            .build())
                    .build();
            builder.setWebpushConfig(webpushConfig);
        } else if (platform == PushPlatform.ANDROID) {
            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                            .setTitle(command.title())
                            .setBody(command.body())
                            .setSound("default")
                            .setChannelId("repairauto_notifications")
                            .build())
                    .build();
            builder.setAndroidConfig(androidConfig);
        } else if (platform == PushPlatform.IOS) {
            ApnsConfig apnsConfig = ApnsConfig.builder()
                    .setAps(Aps.builder()
                            .setSound("default")
                            .setContentAvailable(true)
                            .build())
                    .build();
            builder.setApnsConfig(apnsConfig);
        }
    }

    private PushDeliveryResult classifyFirebaseException(PushEndpoint endpoint, FirebaseMessagingException ex) {
        MessagingErrorCode errorCode = ex.getMessagingErrorCode();
        String codeStr = errorCode != null ? errorCode.name() : "UNKNOWN";

        LOGGER.warn("FCM push delivery failed for endpoint id={}, errorCode={}, message={}",
                endpoint.getId(), codeStr, ex.getMessage());

        if (errorCode == null) {
            return PushDeliveryResult.retryableFailure("FIREBASE_ERROR", ex.getMessage());
        }

        return switch (errorCode) {
            case UNREGISTERED -> {
                LOGGER.info("Classified as PERMANENT_FAILURE: FCM registration token for endpoint id={} is UNREGISTERED", endpoint.getId());
                yield PushDeliveryResult.permanentFailure(codeStr, ex.getMessage());
            }
            case INVALID_ARGUMENT -> {
                LOGGER.warn("Classified as INVALID_PAYLOAD: FCM payload or argument invalid for endpoint id={}", endpoint.getId());
                yield PushDeliveryResult.invalidPayload(codeStr, ex.getMessage());
            }
            case QUOTA_EXCEEDED, UNAVAILABLE, INTERNAL -> {
                LOGGER.warn("Classified as RETRYABLE_FAILURE: FCM error={} for endpoint id={}", codeStr, endpoint.getId());
                yield PushDeliveryResult.retryableFailure(codeStr, ex.getMessage());
            }
            case SENDER_ID_MISMATCH -> {
                LOGGER.error("Classified as CONFIGURATION_FAILURE: SENDER_ID_MISMATCH for endpoint id={}, ownerType={}, clientType={}, platform={}, firebaseAppKey={}. Project ID mismatch between client and backend.",
                        endpoint.getId(), endpoint.getOwnerType(), endpoint.getClientType(), endpoint.getPlatform(), endpoint.getFirebaseAppKey());
                yield PushDeliveryResult.configurationFailure(codeStr, ex.getMessage());
            }
            case THIRD_PARTY_AUTH_ERROR -> {
                LOGGER.error("Classified as CONFIGURATION_FAILURE: THIRD_PARTY_AUTH_ERROR (APNs auth credentials/VAPID key error) for endpoint id={}, ownerType={}, clientType={}, platform={}, firebaseAppKey={}",
                        endpoint.getId(), endpoint.getOwnerType(), endpoint.getClientType(), endpoint.getPlatform(), endpoint.getFirebaseAppKey());
                yield PushDeliveryResult.configurationFailure(codeStr, ex.getMessage());
            }
        };
    }
}
