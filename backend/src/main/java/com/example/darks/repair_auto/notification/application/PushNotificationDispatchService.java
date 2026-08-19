package com.example.darks.repair_auto.notification.application;

import com.example.darks.repair_auto.notification.domain.NotificationOutbox;
import com.example.darks.repair_auto.notification.domain.NotificationPushDelivery;
import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
import com.example.darks.repair_auto.notification.domain.NotificationStatus;
import com.example.darks.repair_auto.notification.infrastructure.persistence.NotificationPushDeliveryRepository;
import com.example.darks.repair_auto.notification.push.application.PushEndpointService;
import com.example.darks.repair_auto.notification.push.config.FirebasePushProperties;
import com.example.darks.repair_auto.notification.push.domain.PushEndpoint;
import com.example.darks.repair_auto.notification.push.gateway.PushDeliveryCommand;
import com.example.darks.repair_auto.notification.push.gateway.PushDeliveryGateway;
import com.example.darks.repair_auto.notification.push.gateway.PushDeliveryResult;
import com.example.darks.repair_auto.notification.push.gateway.PushDeliveryStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PushNotificationDispatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushNotificationDispatchService.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final PushEndpointService pushEndpointService;
    private final PushDeliveryGateway pushDeliveryGateway;
    private final NotificationPushDeliveryRepository pushDeliveryRepository;
    private final NotificationRetryPolicy retryPolicy;
    private final FirebasePushProperties firebasePushProperties;
    private final MeterRegistry meterRegistry;
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public PushNotificationDispatchService(
            PushEndpointService pushEndpointService,
            PushDeliveryGateway pushDeliveryGateway,
            NotificationPushDeliveryRepository pushDeliveryRepository,
            NotificationRetryPolicy retryPolicy,
            FirebasePushProperties firebasePushProperties,
            MeterRegistry meterRegistry) {
        this(pushEndpointService, pushDeliveryGateway, pushDeliveryRepository, retryPolicy, firebasePushProperties, meterRegistry, Clock.systemUTC());
    }

    public PushNotificationDispatchService(
            PushEndpointService pushEndpointService,
            PushDeliveryGateway pushDeliveryGateway,
            NotificationPushDeliveryRepository pushDeliveryRepository,
            NotificationRetryPolicy retryPolicy,
            FirebasePushProperties firebasePushProperties) {
        this(pushEndpointService, pushDeliveryGateway, pushDeliveryRepository, retryPolicy, firebasePushProperties, null, Clock.systemUTC());
    }

    public PushNotificationDispatchService(
            PushEndpointService pushEndpointService,
            PushDeliveryGateway pushDeliveryGateway,
            NotificationPushDeliveryRepository pushDeliveryRepository,
            NotificationRetryPolicy retryPolicy,
            FirebasePushProperties firebasePushProperties,
            Clock clock) {
        this(pushEndpointService, pushDeliveryGateway, pushDeliveryRepository, retryPolicy, firebasePushProperties, null, clock);
    }

    public PushNotificationDispatchService(
            PushEndpointService pushEndpointService,
            PushDeliveryGateway pushDeliveryGateway,
            NotificationPushDeliveryRepository pushDeliveryRepository,
            NotificationRetryPolicy retryPolicy,
            FirebasePushProperties firebasePushProperties,
            MeterRegistry meterRegistry,
            Clock clock) {
        this.pushEndpointService = pushEndpointService;
        this.pushDeliveryGateway = pushDeliveryGateway;
        this.pushDeliveryRepository = pushDeliveryRepository;
        this.retryPolicy = retryPolicy;
        this.firebasePushProperties = firebasePushProperties;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    public NotificationDeliveryResult dispatch(NotificationOutbox notification) {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);

        if (!firebasePushProperties.enabled()) {
            LOGGER.info("Push delivery skipped for notification id={}: Firebase push is disabled", notification.getId());
            return NotificationDeliveryResult.unavailable("PUSH_DISABLED");
        }

        List<PushEndpoint> endpoints = resolveEndpoints(notification.getRecipientType(), notification.getRecipientId());
        List<NotificationPushDelivery> existingDeliveries = pushDeliveryRepository.findByNotificationOutboxId(notification.getId());

        if (endpoints.isEmpty() && existingDeliveries.isEmpty()) {
            LOGGER.info("Push delivery skipped for notification id={}: No active endpoints found for recipientType={}, recipientId={}",
                    notification.getId(), notification.getRecipientType(), notification.getRecipientId());
            return NotificationDeliveryResult.unavailable("NO_ACTIVE_ENDPOINTS");
        }

        Map<String, String> semanticData = extractSemanticData(notification);
        Long repairRequestId = extractRepairRequestId(notification, semanticData);
        if (repairRequestId != null) {
            semanticData.put("repairRequestId", String.valueOf(repairRequestId));
            semanticData.putIfAbsent("target", "REPAIR_REQUEST_DETAILS");
        }
        String requestNumber = semanticData.get("requestNumber");

        List<NotificationPushDelivery> processedDeliveries = new ArrayList<>();
        for (PushEndpoint endpoint : endpoints) {
            NotificationPushDelivery delivery = pushDeliveryRepository
                    .findByNotificationOutboxIdAndPushEndpointId(notification.getId(), endpoint.getId())
                    .orElseGet(() -> new NotificationPushDelivery(notification, endpoint, now));

            if (delivery.getStatus() == NotificationStatus.DELIVERED || delivery.getStatus() == NotificationStatus.DEAD) {
                processedDeliveries.add(delivery);
                continue;
            }

            PushDeliveryCommand command = new PushDeliveryCommand(
                    endpoint,
                    notification.getRenderedTitle(),
                    notification.getRenderedMessage(),
                    notification.getNotificationType().name(),
                    notification.getId(),
                    repairRequestId,
                    requestNumber,
                    null,
                    semanticData);

            PushDeliveryResult result = pushDeliveryGateway.deliver(command);
            recordDeliveryMetric(endpoint.getPlatform().name().toLowerCase(Locale.ROOT), result.status().name().toLowerCase(Locale.ROOT));

            if (result.isSuccess()) {
                delivery.markDelivered(now, result.firebaseMessageId());
            } else if (result.shouldDisableEndpoint()) {
                LOGGER.info("Disabling dead push endpoint id={} following UNREGISTERED result", endpoint.getId());
                pushEndpointService.disableInvalidEndpoint(endpoint.getId());
                delivery.markDead(now, result.errorCode(), "UNREGISTERED");
            } else if (result.status() == PushDeliveryStatus.INVALID_PAYLOAD) {
                delivery.markDead(now, result.errorCode(), "INVALID_PAYLOAD");
            } else {
                OffsetDateTime nextAttemptAt = now.plus(retryPolicy.nextBackoff(delivery.getAttemptCount() + 1));
                delivery.markRetry(now, nextAttemptAt, result.errorCode(), result.status().name());
            }

            NotificationPushDelivery saved = pushDeliveryRepository.saveAndFlush(delivery);
            processedDeliveries.add(saved != null ? saved : delivery);
        }

        return aggregatePushDeliveryState(processedDeliveries, now, notification.getAttemptCount());
    }

    public NotificationDeliveryResult aggregatePushDeliveryState(
            List<NotificationPushDelivery> deliveries,
            OffsetDateTime now,
            int parentAttemptCount) {
        if (deliveries == null || deliveries.isEmpty()) {
            return NotificationDeliveryResult.unavailable("NO_ACTIVE_ENDPOINTS");
        }

        boolean anyRetry = deliveries.stream().anyMatch(d -> d.getStatus() == NotificationStatus.RETRY_SCHEDULED);
        boolean anySuccess = deliveries.stream().anyMatch(d -> d.getStatus() == NotificationStatus.DELIVERED);
        boolean allDead = deliveries.stream().allMatch(d -> d.getStatus() == NotificationStatus.DEAD);

        if (anyRetry) {
            OffsetDateTime earliestRetry = deliveries.stream()
                    .filter(d -> d.getStatus() == NotificationStatus.RETRY_SCHEDULED)
                    .map(NotificationPushDelivery::getNextAttemptAt)
                    .filter(Objects::nonNull)
                    .min(OffsetDateTime::compareTo)
                    .orElse(now.plus(retryPolicy.nextBackoff(parentAttemptCount + 1)));

            return NotificationDeliveryResult.transientFailure("PUSH_RETRYABLE_FAILURE", earliestRetry);
        }

        if (anySuccess) {
            String firstSuccessId = deliveries.stream()
                    .map(NotificationPushDelivery::getFirebaseMessageId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse("fcm-batch-success");
            return NotificationDeliveryResult.delivered(firstSuccessId);
        }

        if (allDead) {
            return NotificationDeliveryResult.permanentFailure("ALL_ENDPOINTS_FAILED");
        }

        return NotificationDeliveryResult.unavailable("NO_DELIVERY_EXECUTED");
    }

    private void recordDeliveryMetric(String platform, String outcome) {
        if (meterRegistry != null) {
            Counter.builder("repairauto.push.delivery")
                    .tag("platform", platform)
                    .tag("outcome", outcome)
                    .register(meterRegistry)
                    .increment();
        }
    }

    private List<PushEndpoint> resolveEndpoints(NotificationRecipientType recipientType, Long recipientId) {
        if (recipientId == null || recipientType == null) {
            return List.of();
        }
        return switch (recipientType) {
            case STAFF -> pushEndpointService.findEnabledForStaff(recipientId);
            case CUSTOMER -> pushEndpointService.findEnabledForCustomer(recipientId);
            case TECHNICIAN -> pushEndpointService.findEnabledForTechnician(recipientId);
        };
    }

    private Map<String, String> extractSemanticData(NotificationOutbox notification) {
        Map<String, String> data = new HashMap<>();
        String payloadJson = notification.getPayloadJson();
        if (payloadJson == null || payloadJson.isBlank()) {
            return data;
        }

        try {
            Map<String, Object> map = objectMapper.readValue(payloadJson, MAP_TYPE);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() != null) {
                    data.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to parse payloadJson for notification id={}: {}", notification.getId(), e.getMessage());
        }
        return data;
    }

    private Long extractRepairRequestId(NotificationOutbox notification, Map<String, String> semanticData) {
        if (notification.getRepairRequest() != null && notification.getRepairRequest().getId() != null) {
            return notification.getRepairRequest().getId();
        }
        String requestIdStr = semanticData.get("repairRequestId");
        if (requestIdStr == null) {
            requestIdStr = semanticData.get("requestId");
        }
        if (requestIdStr != null) {
            try {
                return Long.parseLong(requestIdStr);
            } catch (NumberFormatException ignored) {
                // Ignore parsing errors
            }
        }
        return null;
    }
}
