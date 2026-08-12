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
                "Request was created for {category}. Status: {status}.",
                "Заявка создана для категории {category}. Статус: {status}.",
                "{category} bo'yicha so'rov yaratildi. Holat: {status}.");
        put(NotificationType.CUSTOMER_TECHNICIAN_ASSIGNED,
                "Technician {technicianName} was assigned. Visit: {scheduledVisitAt}.",
                "Мастер {technicianName} назначен. Визит: {scheduledVisitAt}.",
                "{technicianName} biriktirildi. Tashrif: {scheduledVisitAt}.");
        put(NotificationType.CUSTOMER_TECHNICIAN_REASSIGNED,
                "Request was reassigned to technician {technicianName}. Visit: {scheduledVisitAt}.",
                "Заявка переназначена мастеру {technicianName}. Визит: {scheduledVisitAt}.",
                "So'rov {technicianName} ustaga qayta biriktirildi. Tashrif: {scheduledVisitAt}.");
        put(NotificationType.CUSTOMER_TECHNICIAN_UNASSIGNED,
                "Request is waiting for a technician again.",
                "Заявка снова ожидает назначения мастера.",
                "So'rov yana usta biriktirilishini kutmoqda.");
        put(NotificationType.CUSTOMER_VISIT_SCHEDULED,
                "Visit is scheduled for {scheduledVisitAt}.",
                "Визит назначен на {scheduledVisitAt}.",
                "Tashrif {scheduledVisitAt} vaqtiga belgilandi.");
        put(NotificationType.CUSTOMER_VISIT_RESCHEDULED,
                "Visit was moved to {scheduledVisitAt}.",
                "Визит перенесен на {scheduledVisitAt}.",
                "Tashrif {scheduledVisitAt} vaqtiga ko'chirildi.");
        put(NotificationType.CUSTOMER_VISIT_SCHEDULE_CLEARED,
                "Visit time is no longer confirmed.",
                "Время визита больше не подтверждено.",
                "Tashrif vaqti hozircha tasdiqlanmagan.");
        put(NotificationType.CUSTOMER_REPAIR_STARTED,
                "Repair is now {status}.",
                "Ремонт теперь {status}.",
                "Ta'mirlash holati: {status}.");
        put(NotificationType.CUSTOMER_WAITING_FOR_PARTS,
                "Repair is {status}. Required parts are being arranged.",
                "Ремонт {status}. Необходимые запчасти подготавливаются.",
                "Ta'mirlash {status}. Kerakli qismlar tayyorlanmoqda.");
        put(NotificationType.CUSTOMER_REPAIR_RESUMED,
                "Repair resumed and is {status}.",
                "Ремонт возобновлен, статус: {status}.",
                "Ta'mirlash davom ettirildi, holat: {status}.");
        put(NotificationType.CUSTOMER_REPAIR_COMPLETED,
                "Repair is {status}. You can leave a review in the bot.",
                "Ремонт {status}.",
                "Ta'mirlash {status}. Botda sharh qoldirishingiz mumkin.");
        put(NotificationType.CUSTOMER_REPAIR_COMPLETED,
                "Repair is {status}. You can leave a review in the bot.",
                "Ремонт {status}. Вы можете оставить отзыв в боте.",
                "Ta'mirlash {status}. Botda sharh qoldirishingiz mumkin.");
        put(NotificationType.CUSTOMER_REQUEST_CANCELLED,
                "Repair is {status}.",
                "Ремонт {status}.",
                "Ta'mirlash {status}.");
        put(NotificationType.TECHNICIAN_NEW_ASSIGNMENT,
                "New job: {category}. Priority: {priority}. Visit: {scheduledVisitAt}. Open Pending.",
                "Новая работа: {category}. Приоритет: {priority}. Визит: {scheduledVisitAt}. Откройте Ожидающие.",
                "Yangi ish: {category}. Muhimlik: {priority}. Tashrif: {scheduledVisitAt}. Kutilayotgan bo'limini oching.");
        put(NotificationType.TECHNICIAN_REASSIGNED_TO_REQUEST,
                "Job was reassigned to you. Priority: {priority}. Visit: {scheduledVisitAt}. Open Pending.",
                "Заявка переназначена вам. Приоритет: {priority}. Визит: {scheduledVisitAt}. Откройте Ожидающие.",
                "Ish sizga qayta biriktirildi. Muhimlik: {priority}. Tashrif: {scheduledVisitAt}. Kutilayotgan bo'limini oching.");
        put(NotificationType.TECHNICIAN_REMOVED_FROM_REQUEST,
                "You were removed from the job.",
                "Вы сняты с заявки.",
                "Siz ishdan olib tashlandingiz.");
        put(NotificationType.TECHNICIAN_VISIT_SCHEDULED,
                "Visit for job is scheduled for {scheduledVisitAt}. Open Active.",
                "Визит по заявке назначен на {scheduledVisitAt}. Откройте Активные.",
                "Ish bo'yicha tashrif {scheduledVisitAt} vaqtiga belgilandi. Faol bo'limini oching.");
        put(NotificationType.TECHNICIAN_VISIT_RESCHEDULED,
                "Visit for job was moved to {scheduledVisitAt}. Open Active.",
                "Визит по заявке перенесен на {scheduledVisitAt}. Откройте Активные.",
                "Ish bo'yicha tashrif {scheduledVisitAt} vaqtiga ko'chirildi. Faol bo'limini oching.");
        put(NotificationType.TECHNICIAN_VISIT_SCHEDULE_CLEARED,
                "Visit time for job is no longer confirmed.",
                "Время визита по заявке больше не подтверждено.",
                "Ish bo'yicha tashrif vaqti hozircha tasdiqlanmagan.");
        put(NotificationType.TECHNICIAN_REQUEST_CANCELLED,
                "Job was cancelled.",
                "Заявка отменена.",
                "Ish bekor qilindi.");
    }

    private void put(NotificationType type, String en, String ru, String uz) {
        Map<LanguageCode, String> byLanguage = new EnumMap<>(LanguageCode.class);
        byLanguage.put(LanguageCode.EN, en);
        byLanguage.put(LanguageCode.RU, ru);
        byLanguage.put(LanguageCode.UZ, uz);
        templates.put(type, byLanguage);
    }
}
