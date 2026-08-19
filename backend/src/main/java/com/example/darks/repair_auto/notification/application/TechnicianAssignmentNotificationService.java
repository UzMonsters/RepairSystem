package com.example.darks.repair_auto.notification.application;

import com.example.darks.repair_auto.notification.domain.NotificationFailureCategory;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentStatus;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.repair.attachment.domain.RepairAttachment;
import com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.ObjectStorageService;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StoredObjectDownload;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.telegram.core.application.TelegramApiException;
import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import com.example.darks.repair_auto.telegram.core.application.TelegramMediaPhoto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class TechnicianAssignmentNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TechnicianAssignmentNotificationService.class);

    private final RepairRequestRepository requestRepository;
    private final RepairAttachmentRepository attachmentRepository;
    private final ObjectStorageService objectStorageService;
    private final TelegramBotClient botClient;
    private final NotificationTemplateService templateService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TechnicianAssignmentNotificationService(
            RepairRequestRepository requestRepository,
            RepairAttachmentRepository attachmentRepository,
            ObjectStorageService objectStorageService,
            @Qualifier("technicianTelegramBotClient") TelegramBotClient botClient,
            NotificationTemplateService templateService) {
        this.requestRepository = requestRepository;
        this.attachmentRepository = attachmentRepository;
        this.objectStorageService = objectStorageService;
        this.botClient = botClient;
        this.templateService = templateService;
    }

    public NotificationDeliveryResult deliverAssignment(
            ClaimedNotification notification,
            NotificationRecipientResolver.ResolvedRecipient recipient) {

        Optional<RepairRequest> optionalRequest = resolveRequest(notification);
        if (optionalRequest.isEmpty()) {
            return deliverFallback(notification, recipient);
        }

        RepairRequest request = optionalRequest.get();
        LanguageCode language = recipient.language();

        // 1. Send Request Summary
        String summaryText = buildSummaryText(request, language);
        try {
            botClient.sendMessage(recipient.chatId(), summaryText, null);
        } catch (TelegramApiException exception) {
            LOGGER.warn("Failed to send assignment summary to technician chatId={}", recipient.chatId(), exception);
            return mapFailure(exception);
        }

        // 2. Send Customer Problem Photos (Safe & Isolated)
        try {
            sendRequestPhotos(request, recipient.chatId());
        } catch (Exception exception) {
            LOGGER.warn("Failed to deliver request photos for request {} to technician chatId={}",
                    request.getId(), recipient.chatId(), exception);
        }

        // 3. Send Request Location (Safe & Isolated)
        try {
            sendRequestLocation(request, recipient.chatId());
        } catch (Exception exception) {
            LOGGER.warn("Failed to deliver request location for request {} to technician chatId={}",
                    request.getId(), recipient.chatId(), exception);
        }

        // 4. Send Decision Message with Accept / Reject buttons
        String decisionText = buildDecisionQuestion(language);
        String decisionMarkup = buildDecisionKeyboard(request.getId(), language);
        try {
            botClient.sendMessage(recipient.chatId(), decisionText, decisionMarkup);
            return NotificationDeliveryResult.delivered();
        } catch (TelegramApiException exception) {
            LOGGER.warn("Failed to send assignment decision message to technician chatId={}",
                    recipient.chatId(), exception);
            return mapFailure(exception);
        }
    }

    private Optional<RepairRequest> resolveRequest(ClaimedNotification notification) {
        if (notification.repairRequestId() != null) {
            return requestRepository.findWithRelationsById(notification.repairRequestId())
                    .or(() -> requestRepository.findById(notification.repairRequestId()));
        }
        if (notification.payloadJson() != null && !notification.payloadJson().isBlank()) {
            try {
                JsonNode node = objectMapper.readTree(notification.payloadJson());
                if (node.has("requestId")) {
                    Long requestId = Long.parseLong(node.get("requestId").asText());
                    return requestRepository.findWithRelationsById(requestId)
                            .or(() -> requestRepository.findById(requestId));
                }
            } catch (Exception e) {
                LOGGER.debug("Could not parse requestId from notification payload", e);
            }
        }
        return Optional.empty();
    }

    private NotificationDeliveryResult deliverFallback(
            ClaimedNotification notification,
            NotificationRecipientResolver.ResolvedRecipient recipient) {
        String text = templateService.renderTelegramText(new NotificationTemplateService.RenderedNotification(
                recipient.language(),
                notification.renderedTitle(),
                notification.renderedMessage()));
        try {
            botClient.sendMessage(recipient.chatId(), text, null);
            return NotificationDeliveryResult.delivered();
        } catch (TelegramApiException exception) {
            return mapFailure(exception);
        }
    }

    private String buildSummaryText(RepairRequest request, LanguageCode language) {
        String header = switch (language) {
            case EN -> "🔧 New repair request";
            case RU -> "🔧 Новая заявка на ремонт";
            case UZ -> "🔧 Yangi ta'mirlash arizasi";
        };
        String requestLabel = switch (language) {
            case EN -> "Request: " + request.getRequestNumber();
            case RU -> "Заявка: " + request.getRequestNumber();
            case UZ -> "Ariza: " + request.getRequestNumber();
        };
        String categoryName = switch (language) {
            case EN -> request.getCategory().getNameEn();
            case RU -> request.getCategory().getNameRu();
            case UZ -> request.getCategory().getNameUz();
        };
        String categoryLabel = switch (language) {
            case EN -> "Category: " + categoryName;
            case RU -> "Категория: " + categoryName;
            case UZ -> "Kategoriya: " + categoryName;
        };
        String problemSection = switch (language) {
            case EN -> "Problem:\n" + (request.getDescription() == null ? "" : request.getDescription().trim());
            case RU -> "Проблема:\n" + (request.getDescription() == null ? "" : request.getDescription().trim());
            case UZ -> "Muammo:\n" + (request.getDescription() == null ? "" : request.getDescription().trim());
        };

        StringBuilder sb = new StringBuilder();
        sb.append(header).append("\n\n");
        sb.append(requestLabel).append("\n");
        sb.append(categoryLabel).append("\n\n");
        sb.append(problemSection);

        if (request.getLocationAddress() != null && !request.getLocationAddress().isBlank()) {
            String addressSection = switch (language) {
                case EN -> "Address:\n" + request.getLocationAddress().trim();
                case RU -> "Адрес:\n" + request.getLocationAddress().trim();
                case UZ -> "Manzil:\n" + request.getLocationAddress().trim();
            };
            sb.append("\n\n").append(addressSection);
        }
        return sb.toString();
    }

    private void sendRequestPhotos(RepairRequest request, Long chatId) {
        List<RepairAttachment> attachments = attachmentRepository
                .findByRepairRequestIdAndStatusAndAttachmentTypeInOrderByUploadedAtAsc(
                        request.getId(),
                        AttachmentStatus.AVAILABLE,
                        List.of(AttachmentType.CUSTOMER_PROBLEM_PHOTO));
        if (attachments.isEmpty()) {
            return;
        }

        List<TelegramMediaPhoto> mediaPhotos = new ArrayList<>();
        for (RepairAttachment attachment : attachments) {
            try {
                StoredObjectDownload download = objectStorageService.download(attachment.getStorageKey());
                if (download != null && download.inputStream() != null) {
                    try (InputStream in = download.inputStream()) {
                        byte[] bytes = in.readAllBytes();
                        if (bytes.length > 0) {
                            mediaPhotos.add(new TelegramMediaPhoto(attachment.getOriginalFileName(), bytes));
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to download attachment id={} storageKey={} for technician assignment delivery",
                        attachment.getId(), attachment.getStorageKey(), e);
            }
        }

        if (mediaPhotos.isEmpty()) {
            return;
        }

        if (mediaPhotos.size() == 1) {
            TelegramMediaPhoto single = mediaPhotos.get(0);
            botClient.sendPhoto(chatId, single.filename(), single.bytes(), null);
        } else {
            for (int i = 0; i < mediaPhotos.size(); i += 10) {
                List<TelegramMediaPhoto> chunk = mediaPhotos.subList(i, Math.min(i + 10, mediaPhotos.size()));
                if (chunk.size() == 1) {
                    TelegramMediaPhoto single = chunk.get(0);
                    botClient.sendPhoto(chatId, single.filename(), single.bytes(), null);
                } else {
                    botClient.sendMediaGroup(chatId, chunk);
                }
            }
        }
    }

    private void sendRequestLocation(RepairRequest request, Long chatId) {
        if (request.getLocationLatitude() != null && request.getLocationLongitude() != null) {
            botClient.sendLocation(
                    chatId,
                    request.getLocationLatitude().doubleValue(),
                    request.getLocationLongitude().doubleValue());
        }
    }

    private String buildDecisionQuestion(LanguageCode language) {
        return switch (language) {
            case EN -> "Can you accept this repair?";
            case RU -> "Вы можете принять этот ремонт?";
            case UZ -> "Ushbu ta'mirlashni qabul qilasizmi?";
        };
    }

    private String buildDecisionKeyboard(Long requestId, LanguageCode language) {
        String acceptText = switch (language) {
            case EN -> "✅ Accept";
            case RU -> "✅ Принять";
            case UZ -> "✅ Qabul qilish";
        };
        String rejectText = switch (language) {
            case EN -> "❌ Reject";
            case RU -> "❌ Отклонить";
            case UZ -> "❌ Rad etish";
        };
        return "{\"inline_keyboard\":[[{\"text\":\"" + json(acceptText) + "\",\"callback_data\":\"taccept:"
                + requestId + "\"},{\"text\":\"" + json(rejectText) + "\",\"callback_data\":\"treject:"
                + requestId + "\"}]]}";
    }

    private String json(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private NotificationDeliveryResult mapFailure(TelegramApiException exception) {
        if (isPermanent(exception)) {
            return NotificationDeliveryResult.permanentFailure(
                    NotificationFailureCategory.TELEGRAM_PERMANENT_FAILURE);
        }
        return NotificationDeliveryResult.transientFailure(
                NotificationFailureCategory.TELEGRAM_TRANSIENT_FAILURE);
    }

    private boolean isPermanent(TelegramApiException exception) {
        String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase();
        if (exception.getCause() != null && exception.getCause().getMessage() != null) {
            message = message + " " + exception.getCause().getMessage().toLowerCase();
        }
        return message.contains("blocked")
                || message.contains("chat not found")
                || message.contains("bad request")
                || message.contains("forbidden")
                || message.contains("permanent");
    }
}
