package com.example.darks.repair_auto.notification.application;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
import com.example.darks.repair_auto.notification.domain.NotificationType;
import com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventFactory {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public NotificationEvent customer(
            NotificationType type,
            RepairRequest request,
            String eventKeyPart) {
        return event(
                type,
                NotificationRecipientType.CUSTOMER,
                request.getCustomer().getId(),
                request,
                eventKey(type, request, eventKeyPart, "customer", request.getCustomer().getId()),
                payload(request, null, null));
    }

    public NotificationEvent customer(
            NotificationType type,
            RepairRequest request,
            RepairAssignment assignment,
            String eventKeyPart) {
        return event(
                type,
                NotificationRecipientType.CUSTOMER,
                request.getCustomer().getId(),
                request,
                eventKey(type, request, eventKeyPart, "customer", request.getCustomer().getId()),
                payload(request, assignment.getTechnician(), assignment.getScheduledVisitAt()));
    }

    public NotificationEvent technician(
            NotificationType type,
            RepairRequest request,
            Technician technician,
            OffsetDateTime scheduledVisitAt,
            String eventKeyPart) {
        return event(
                type,
                NotificationRecipientType.TECHNICIAN,
                technician.getId(),
                request,
                eventKey(type, request, eventKeyPart, "technician", technician.getId()),
                payload(request, technician, scheduledVisitAt));
    }

    public NotificationEvent technician(
            NotificationType type,
            RepairRequest request,
            RepairAssignment assignment,
            String eventKeyPart) {
        return technician(type, request, assignment.getTechnician(), assignment.getScheduledVisitAt(), eventKeyPart);
    }

    private NotificationEvent event(
            NotificationType type,
            NotificationRecipientType recipientType,
            Long recipientId,
            RepairRequest request,
            String eventKey,
            String payloadJson) {
        return new NotificationEvent(
                eventKey,
                type,
                recipientType,
                recipientId,
                request,
                templateKey(type),
                payloadJson);
    }

    private String payload(RepairRequest request, Technician technician, OffsetDateTime scheduledVisitAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        RepairCategory category = request.getCategory();
        payload.put("requestId", String.valueOf(request.getId()));
        payload.put("requestNumber", request.getRequestNumber());
        payload.put("categoryNameEn", category.getNameEn());
        payload.put("categoryNameRu", category.getNameRu());
        payload.put("categoryNameUz", category.getNameUz());
        payload.put("priority", request.getPriority().name());
        payload.put("status", request.getStatus().name());
        if (technician != null) {
            payload.put("technicianName", technician.getFullName());
        }
        if (scheduledVisitAt != null) {
            payload.put("scheduledVisitAt", scheduledVisitAt.toInstant().toString());
        }
        return json(payload);
    }

    private String eventKey(
            NotificationType type,
            RepairRequest request,
            String eventKeyPart,
            String recipientKind,
            Long recipientId) {
        return "request:%d:%s:%s:%s:%d".formatted(
                request.getId(),
                type.name().toLowerCase(java.util.Locale.ROOT),
                eventKeyPart,
                recipientKind,
                recipientId);
    }

    private String templateKey(NotificationType type) {
        return "notification." + type.name().toLowerCase(java.util.Locale.ROOT).replace('_', '.');
    }

    private String json(Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            if (json.length() > 4000) {
                throw new IllegalArgumentException("Notification payload is too large.");
            }
            return json;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Notification payload could not be encoded.", exception);
        }
    }

    public record NotificationEvent(
            String eventKey,
            NotificationType type,
            NotificationRecipientType recipientType,
            Long recipientId,
            RepairRequest repairRequest,
            String templateKey,
            String payloadJson) {
    }

    public static String statusEventKeyPart(Long historyId, RepairRequestStatus status) {
        if (historyId != null) {
            return "status-history:%d".formatted(historyId);
        }
        return "status:%s".formatted(status.name().toLowerCase(java.util.Locale.ROOT));
    }
}
