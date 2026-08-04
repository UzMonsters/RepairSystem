package com.example.darks.repair_auto.notification.application;

import com.example.darks.repair_auto.notification.domain.NotificationType;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationTemplateService {

    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {
    };
    private static final DateTimeFormatter TELEGRAM_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final int TELEGRAM_TEXT_LIMIT = 4096;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<NotificationType, Map<LanguageCode, String>> templates = new EnumMap<>(NotificationType.class);
    private final DateTimeFormatter telegramDateFormatter;

    public NotificationTemplateService(ZoneId businessZone) {
        this.telegramDateFormatter = TELEGRAM_DATE_FORMATTER.withZone(businessZone);
        loadTemplates();
    }

    public String render(NotificationType type, String payloadJson, int payloadVersion, LanguageCode language) {
        if (payloadVersion != 1) {
            throw new IllegalArgumentException("Unsupported notification payload version.");
        }
        LanguageCode safeLanguage = language == null ? LanguageCode.UZ : language;
        Map<String, String> payload = payload(payloadJson);
        String template = templates.getOrDefault(type, Map.of()).get(safeLanguage);
        if (template == null) {
            throw new IllegalArgumentException("Missing notification template.");
        }
        String text = template
                .replace("{requestNumber}", value(payload, "requestNumber"))
                .replace("{category}", category(payload, safeLanguage))
                .replace("{technicianName}", value(payload, "technicianName"))
                .replace("{scheduledVisitAt}", scheduledVisitAt(payload, safeLanguage))
                .replace("{priority}", priority(payload, safeLanguage))
                .replace("{status}", status(payload, safeLanguage));
        if (text.length() > TELEGRAM_TEXT_LIMIT) {
            return text.substring(0, TELEGRAM_TEXT_LIMIT - 1);
        }
        return text;
    }

    public boolean hasTemplate(NotificationType type, LanguageCode language) {
        return templates.containsKey(type) && templates.get(type).containsKey(language);
    }

    private Map<String, String> payload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, MAP_TYPE);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Notification payload could not be decoded.", exception);
        }
    }

    private String category(Map<String, String> payload, LanguageCode language) {
        return switch (language) {
            case EN -> value(payload, "categoryNameEn");
            case RU -> value(payload, "categoryNameRu");
            case UZ -> value(payload, "categoryNameUz");
        };
    }

    private String scheduledVisitAt(Map<String, String> payload, LanguageCode language) {
        String value = payload.get("scheduledVisitAt");
        if (value == null || value.isBlank()) {
            return switchDefault("not scheduled", "не назначено", "belgilanmagan", language);
        }
        return telegramDateFormatter.format(Instant.parse(value));
    }

    private String priority(Map<String, String> payload, LanguageCode language) {
        try {
            RepairRequestPriority priority = RepairRequestPriority.valueOf(payload.getOrDefault("priority", "NORMAL"));
            return switch (language) {
                case EN -> switch (priority) {
                    case LOW -> "low";
                    case NORMAL -> "normal";
                    case HIGH -> "high";
                    case URGENT -> "urgent";
                };
                case RU -> switch (priority) {
                    case LOW -> "низкий";
                    case NORMAL -> "обычный";
                    case HIGH -> "высокий";
                    case URGENT -> "срочный";
                };
                case UZ -> switch (priority) {
                    case LOW -> "past";
                    case NORMAL -> "odatdagi";
                    case HIGH -> "yuqori";
                    case URGENT -> "shoshilinch";
                };
            };
        } catch (RuntimeException exception) {
            return "";
        }
    }

    private String status(Map<String, String> payload, LanguageCode language) {
        try {
            RepairRequestStatus status = RepairRequestStatus.valueOf(payload.getOrDefault("status", "NEW"));
            return switch (language) {
                case EN -> switch (status) {
                    case NEW -> "new";
                    case ASSIGNED -> "assigned";
                    case SCHEDULED -> "scheduled";
                    case IN_PROGRESS -> "in progress";
                    case WAITING_FOR_PARTS -> "waiting for parts";
                    case COMPLETED -> "completed";
                    case CANCELLED -> "cancelled";
                };
                case RU -> switch (status) {
                    case NEW -> "новая";
                    case ASSIGNED -> "назначена";
                    case SCHEDULED -> "запланирована";
                    case IN_PROGRESS -> "в работе";
                    case WAITING_FOR_PARTS -> "ожидает запчасти";
                    case COMPLETED -> "завершена";
                    case CANCELLED -> "отменена";
                };
                case UZ -> switch (status) {
                    case NEW -> "yangi";
                    case ASSIGNED -> "biriktirilgan";
                    case SCHEDULED -> "rejalashtirilgan";
                    case IN_PROGRESS -> "jarayonda";
                    case WAITING_FOR_PARTS -> "ehtiyot qismlar kutilmoqda";
                    case COMPLETED -> "yakunlangan";
                    case CANCELLED -> "bekor qilingan";
                };
            };
        } catch (RuntimeException exception) {
            return "";
        }
    }

    private String value(Map<String, String> payload, String key) {
        return escape(payload.getOrDefault(key, ""));
    }

    private String escape(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String switchDefault(String en, String ru, String uz, LanguageCode language) {
        return switch (language) {
            case EN -> en;
            case RU -> ru;
            case UZ -> uz;
        };
    }

    private void loadTemplates() {
        put(NotificationType.CUSTOMER_REQUEST_CREATED,
                "Request {requestNumber} was created for {category}. Status: {status}.",
                "Заявка {requestNumber} создана для категории {category}. Статус: {status}.",
                "{category} bo'yicha {requestNumber} so'rovi yaratildi. Holat: {status}.");
        put(NotificationType.CUSTOMER_TECHNICIAN_ASSIGNED,
                "Technician {technicianName} was assigned to request {requestNumber}. Visit: {scheduledVisitAt}.",
                "Мастер {technicianName} назначен на заявку {requestNumber}. Визит: {scheduledVisitAt}.",
                "{requestNumber} so'roviga {technicianName} biriktirildi. Tashrif: {scheduledVisitAt}.");
        put(NotificationType.CUSTOMER_TECHNICIAN_REASSIGNED,
                "Request {requestNumber} was reassigned to technician {technicianName}. Visit: {scheduledVisitAt}.",
                "Заявка {requestNumber} переназначена мастеру {technicianName}. Визит: {scheduledVisitAt}.",
                "{requestNumber} so'rovi {technicianName} ustaga qayta biriktirildi. Tashrif: {scheduledVisitAt}.");
        put(NotificationType.CUSTOMER_TECHNICIAN_UNASSIGNED,
                "Request {requestNumber} is waiting for a technician again.",
                "Заявка {requestNumber} снова ожидает назначения мастера.",
                "{requestNumber} so'rovi yana usta biriktirilishini kutmoqda.");
        put(NotificationType.CUSTOMER_VISIT_SCHEDULED,
                "Visit for request {requestNumber} is scheduled for {scheduledVisitAt}.",
                "Визит по заявке {requestNumber} назначен на {scheduledVisitAt}.",
                "{requestNumber} so'rovi bo'yicha tashrif {scheduledVisitAt} vaqtiga belgilandi.");
        put(NotificationType.CUSTOMER_VISIT_RESCHEDULED,
                "Visit for request {requestNumber} was moved to {scheduledVisitAt}.",
                "Визит по заявке {requestNumber} перенесен на {scheduledVisitAt}.",
                "{requestNumber} so'rovi bo'yicha tashrif {scheduledVisitAt} vaqtiga ko'chirildi.");
        put(NotificationType.CUSTOMER_VISIT_SCHEDULE_CLEARED,
                "Visit time for request {requestNumber} is no longer confirmed.",
                "Время визита по заявке {requestNumber} больше не подтверждено.",
                "{requestNumber} so'rovi bo'yicha tashrif vaqti hozircha tasdiqlanmagan.");
        put(NotificationType.CUSTOMER_REPAIR_STARTED,
                "Repair request {requestNumber} is now {status}.",
                "Заявка {requestNumber} теперь {status}.",
                "{requestNumber} so'rovi holati: {status}.");
        put(NotificationType.CUSTOMER_WAITING_FOR_PARTS,
                "Repair request {requestNumber} is {status}. Required parts are being arranged.",
                "Заявка {requestNumber} {status}. Необходимые запчасти подготавливаются.",
                "{requestNumber} so'rovi {status}. Kerakli qismlar tayyorlanmoqda.");
        put(NotificationType.CUSTOMER_REPAIR_RESUMED,
                "Repair request {requestNumber} resumed and is {status}.",
                "Работа по заявке {requestNumber} возобновлена, статус: {status}.",
                "{requestNumber} so'rovi davom ettirildi, holat: {status}.");
        put(NotificationType.CUSTOMER_REPAIR_COMPLETED,
                "Repair request {requestNumber} is {status}. You can leave a review in the bot.",
                "Заявка {requestNumber} {status}.",
                "{requestNumber} so'rovi {status}. Botda sharh qoldirishingiz mumkin.");
        put(NotificationType.CUSTOMER_REPAIR_COMPLETED,
                "Repair request {requestNumber} is {status}. You can leave a review in the bot.",
                "Заявка {requestNumber} {status}. Вы можете оставить отзыв в боте.",
                "{requestNumber} so'rovi {status}. Botda sharh qoldirishingiz mumkin.");
        put(NotificationType.CUSTOMER_REQUEST_CANCELLED,
                "Repair request {requestNumber} is {status}.",
                "Заявка {requestNumber} {status}.",
                "{requestNumber} so'rovi {status}.");
        put(NotificationType.TECHNICIAN_NEW_ASSIGNMENT,
                "New job {requestNumber}: {category}. Priority: {priority}. Visit: {scheduledVisitAt}. Open My Jobs.",
                "Новая работа {requestNumber}: {category}. Приоритет: {priority}. Визит: {scheduledVisitAt}. Откройте Мои заявки.",
                "Yangi ish {requestNumber}: {category}. Muhimlik: {priority}. Tashrif: {scheduledVisitAt}. Mening ishlarimni oching.");
        put(NotificationType.TECHNICIAN_REASSIGNED_TO_REQUEST,
                "Job {requestNumber} was reassigned to you. Priority: {priority}. Visit: {scheduledVisitAt}. Open My Jobs.",
                "Заявка {requestNumber} переназначена вам. Приоритет: {priority}. Визит: {scheduledVisitAt}. Откройте Мои заявки.",
                "{requestNumber} ishi sizga qayta biriktirildi. Muhimlik: {priority}. Tashrif: {scheduledVisitAt}. Mening ishlarimni oching.");
        put(NotificationType.TECHNICIAN_REMOVED_FROM_REQUEST,
                "You were removed from job {requestNumber}.",
                "Вы сняты с заявки {requestNumber}.",
                "Siz {requestNumber} ishidan olib tashlandingiz.");
        put(NotificationType.TECHNICIAN_VISIT_SCHEDULED,
                "Visit for job {requestNumber} is scheduled for {scheduledVisitAt}.",
                "Визит по заявке {requestNumber} назначен на {scheduledVisitAt}.",
                "{requestNumber} ishi bo'yicha tashrif {scheduledVisitAt} vaqtiga belgilandi.");
        put(NotificationType.TECHNICIAN_VISIT_RESCHEDULED,
                "Visit for job {requestNumber} was moved to {scheduledVisitAt}.",
                "Визит по заявке {requestNumber} перенесен на {scheduledVisitAt}.",
                "{requestNumber} ishi bo'yicha tashrif {scheduledVisitAt} vaqtiga ko'chirildi.");
        put(NotificationType.TECHNICIAN_VISIT_SCHEDULE_CLEARED,
                "Visit time for job {requestNumber} is no longer confirmed.",
                "Время визита по заявке {requestNumber} больше не подтверждено.",
                "{requestNumber} ishi bo'yicha tashrif vaqti hozircha tasdiqlanmagan.");
        put(NotificationType.TECHNICIAN_REQUEST_CANCELLED,
                "Job {requestNumber} was cancelled.",
                "Заявка {requestNumber} отменена.",
                "{requestNumber} ishi bekor qilindi.");
    }

    private void put(NotificationType type, String en, String ru, String uz) {
        Map<LanguageCode, String> byLanguage = new EnumMap<>(LanguageCode.class);
        byLanguage.put(LanguageCode.EN, en);
        byLanguage.put(LanguageCode.RU, ru);
        byLanguage.put(LanguageCode.UZ, uz);
        templates.put(type, byLanguage);
    }
}
