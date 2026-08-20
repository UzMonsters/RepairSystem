package com.example.darks.repair_auto.chat.application;

import com.example.darks.repair_auto.chat.domain.ChatMessage;
import com.example.darks.repair_auto.chat.domain.ChatMessageType;
import com.example.darks.repair_auto.chat.domain.Conversation;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.notification.push.application.PushEndpointService;
import com.example.darks.repair_auto.notification.push.config.FirebasePushProperties;
import com.example.darks.repair_auto.notification.push.domain.PushEndpoint;
import com.example.darks.repair_auto.notification.push.gateway.PushDeliveryCommand;
import com.example.darks.repair_auto.notification.push.gateway.PushDeliveryGateway;
import com.example.darks.repair_auto.realtime.event.application.ParticipantRecipient;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ChatPushNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatPushNotificationService.class);
    private static final int MAX_PREVIEW_LENGTH = 100;

    private final PushEndpointService pushEndpointService;
    private final PushDeliveryGateway pushDeliveryGateway;
    private final FirebasePushProperties firebasePushProperties;
    private final CustomerRepository customerRepository;
    private final TechnicianRepository technicianRepository;
    private final UserRepository userRepository;

    public ChatPushNotificationService(
            PushEndpointService pushEndpointService,
            PushDeliveryGateway pushDeliveryGateway,
            FirebasePushProperties firebasePushProperties,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            UserRepository userRepository) {
        this.pushEndpointService = pushEndpointService;
        this.pushDeliveryGateway = pushDeliveryGateway;
        this.firebasePushProperties = firebasePushProperties;
        this.customerRepository = customerRepository;
        this.technicianRepository = technicianRepository;
        this.userRepository = userRepository;
    }

    public void sendPushNotifications(
            Conversation conversation,
            ChatMessage message,
            List<ParticipantRecipient> recipients) {
        if (!firebasePushProperties.enabled()) {
            return;
        }

        if (recipients == null || recipients.isEmpty()) {
            return;
        }

        String senderName = resolveSenderName(message.getSenderType(), message.getSenderId());
        String preview = buildMessagePreview(message);
        Long repairRequestId = conversation.getRepairRequest() != null ? conversation.getRepairRequest().getId() : null;
        String requestNumber = conversation.getRepairRequest() != null ? conversation.getRepairRequest().getRequestNumber() : null;

        Map<String, String> additionalData = new HashMap<>();
        additionalData.put("type", "CHAT_MESSAGE");
        additionalData.put("conversationId", String.valueOf(conversation.getId()));
        additionalData.put("messageId", String.valueOf(message.getId()));
        if (repairRequestId != null) {
            additionalData.put("repairRequestId", String.valueOf(repairRequestId));
        }
        if (requestNumber != null) {
            additionalData.put("requestNumber", requestNumber);
        }
        additionalData.put("senderType", message.getSenderType().name());
        additionalData.put("senderId", String.valueOf(message.getSenderId()));

        for (ParticipantRecipient recipient : recipients) {
            // Do not send push to the sender
            if (recipient.actorType() == message.getSenderType() && recipient.actorId().equals(message.getSenderId())) {
                continue;
            }

            List<PushEndpoint> endpoints = resolveEndpoints(recipient.actorType(), recipient.actorId());
            for (PushEndpoint endpoint : endpoints) {
                try {
                    PushDeliveryCommand command = new PushDeliveryCommand(
                            endpoint,
                            senderName,
                            preview,
                            "CHAT_MESSAGE",
                            null,
                            repairRequestId,
                            requestNumber,
                            "/chat/" + conversation.getId(),
                            additionalData);
                    pushDeliveryGateway.deliver(command);
                } catch (Exception ex) {
                    LOGGER.warn("Failed to send chat push notification to endpoint id={}: {}",
                            endpoint.getId(), ex.getMessage());
                }
            }
        }
    }

    private List<PushEndpoint> resolveEndpoints(ActorType actorType, Long actorId) {
        if (actorType == null || actorId == null) {
            return List.of();
        }
        return switch (actorType) {
            case STAFF -> pushEndpointService.findEnabledForStaff(actorId);
            case CUSTOMER -> pushEndpointService.findEnabledForCustomer(actorId);
            case TECHNICIAN -> pushEndpointService.findEnabledForTechnician(actorId);
        };
    }

    private String resolveSenderName(ActorType senderType, Long senderId) {
        try {
            return switch (senderType) {
                case CUSTOMER -> customerRepository.findById(senderId)
                        .map(c -> c.getFullName() != null && !c.getFullName().isBlank() ? c.getFullName() : "Customer")
                        .orElse("Customer");
                case TECHNICIAN -> technicianRepository.findById(senderId)
                        .map(t -> t.getFullName() != null && !t.getFullName().isBlank() ? t.getFullName() : "Technician")
                        .orElse("Technician");
                case STAFF -> userRepository.findById(senderId)
                        .map(u -> u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : "Support")
                        .orElse("Support");
            };
        } catch (Exception ex) {
            return senderType.name();
        }
    }

    private String buildMessagePreview(ChatMessage message) {
        if (message.getMessageType() == ChatMessageType.IMAGE) {
            return "Photo";
        }
        if (message.getMessageType() == ChatMessageType.FILE) {
            return "Attachment";
        }
        String text = message.getText();
        if (text == null || text.isBlank()) {
            return "New message";
        }
        if (text.length() > MAX_PREVIEW_LENGTH) {
            return text.substring(0, MAX_PREVIEW_LENGTH) + "...";
        }
        return text;
    }
}
