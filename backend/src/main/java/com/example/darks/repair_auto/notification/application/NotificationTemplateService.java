package com.example.darks.repair_auto.notification.application;

import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
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
    private final Map<NotificationType, Map<NotificationRecipientType, Map<LanguageCode, Template>>> templates =
            new EnumMap<>(NotificationType.class);
    private final DateTimeFormatter telegramDateFormatter;

    public NotificationTemplateService(ZoneId businessZone) {
        this.telegramDateFormatter = TELEGRAM_DATE_FORMATTER.withZone(businessZone);
        loadTemplates();
    }

    public RenderedNotification render(
            NotificationType type,
            NotificationRecipientType recipientType,
            String payloadJson,
            int payloadVersion,
            LanguageCode language) {
        if (payloadVersion != 1) {
            throw new IllegalArgumentException("Unsupported notification payload version.");
        }
        LanguageCode safeLanguage = language == null ? LanguageCode.UZ : language;
        Template template = templates
                .getOrDefault(type, Map.of())
                .getOrDefault(recipientType, Map.of())
                .get(safeLanguage);
        if (template == null) {
            throw new IllegalArgumentException("Missing notification template.");
        }
        Map<String, String> payload = payload(payloadJson);
        String title = interpolate(template.title(), payload, safeLanguage);
        String message = limit(interpolate(template.message(), payload, safeLanguage));
        return new RenderedNotification(safeLanguage, title, message);
    }

    public String renderTelegramText(RenderedNotification rendered) {
        return limit(rendered.title() + "\n\n" + rendered.message());
    }

    public boolean hasTemplate(NotificationType type, NotificationRecipientType recipientType, LanguageCode language) {
        return templates.containsKey(type)
                && templates.get(type).containsKey(recipientType)
                && templates.get(type).get(recipientType).containsKey(language);
    }

    public boolean hasTemplate(NotificationType type, LanguageCode language) {
        return templates.getOrDefault(type, Map.of()).values().stream()
                .anyMatch(byLanguage -> byLanguage.containsKey(language));
    }

    private String interpolate(String template, Map<String, String> payload, LanguageCode language) {
        return template
                .replace("{requestNumber}", value(payload, "requestNumber"))
                .replace("{category}", category(payload, language))
                .replace("{technicianName}", value(payload, "technicianName"))
                .replace("{scheduledVisitAt}", scheduledVisitAt(payload, language))
                .replace("{priority}", priority(payload, language))
                .replace("{status}", status(payload, language));
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

    private String limit(String value) {
        if (value.length() > TELEGRAM_TEXT_LIMIT) {
            return value.substring(0, TELEGRAM_TEXT_LIMIT - 1);
        }
        return value;
    }

    private void loadTemplates() {
        put(NotificationType.REQUEST_CREATED, NotificationRecipientType.CUSTOMER,
                "Request created", "Repair request {requestNumber} for {category} was created. Status: {status}.",
                "Заявка создана", "Заявка {requestNumber} по категории {category} создана. Статус: {status}.",
                "Ariza yaratildi", "{requestNumber} raqamli {category} bo'yicha ariza yaratildi. Holat: {status}.");
        put(NotificationType.TECHNICIAN_ASSIGNED, NotificationRecipientType.CUSTOMER,
                "Technician assigned", "Technician {technicianName} has been assigned to request {requestNumber}. Visit: {scheduledVisitAt}.",
                "Мастер назначен", "Мастер {technicianName} назначен на заявку {requestNumber}. Визит: {scheduledVisitAt}.",
                "Usta biriktirildi", "{requestNumber} arizasiga {technicianName} biriktirildi. Tashrif: {scheduledVisitAt}.");
        put(NotificationType.TECHNICIAN_ASSIGNED, NotificationRecipientType.TECHNICIAN,
                "New assignment", "A new repair request {requestNumber} was assigned to you. Priority: {priority}. Visit: {scheduledVisitAt}.",
                "Новое назначение", "Вам назначена новая заявка {requestNumber}. Приоритет: {priority}. Визит: {scheduledVisitAt}.",
                "Yangi topshiriq", "Sizga {requestNumber} raqamli yangi ariza biriktirildi. Muhimlik: {priority}. Tashrif: {scheduledVisitAt}.");
        put(NotificationType.TECHNICIAN_UNASSIGNED, NotificationRecipientType.CUSTOMER,
                "Technician removed", "Request {requestNumber} is waiting for a technician again.",
                "Мастер снят", "Заявка {requestNumber} снова ожидает назначения мастера.",
                "Usta olib tashlandi", "{requestNumber} arizasi yana usta biriktirilishini kutmoqda.");
        put(NotificationType.TECHNICIAN_UNASSIGNED, NotificationRecipientType.TECHNICIAN,
                "Assignment removed", "You were removed from repair request {requestNumber}.",
                "Назначение снято", "Вы сняты с заявки {requestNumber}.",
                "Topshiriq olib tashlandi", "Siz {requestNumber} arizasidan olib tashlandingiz.");
        put(NotificationType.VISIT_SCHEDULED, NotificationRecipientType.CUSTOMER,
                "Visit scheduled", "Visit for request {requestNumber} is scheduled for {scheduledVisitAt}.",
                "Визит назначен", "Визит по заявке {requestNumber} назначен на {scheduledVisitAt}.",
                "Tashrif belgilandi", "{requestNumber} arizasi bo'yicha tashrif {scheduledVisitAt} vaqtiga belgilandi.");
        put(NotificationType.VISIT_SCHEDULED, NotificationRecipientType.TECHNICIAN,
                "Visit scheduled", "Visit for request {requestNumber} is scheduled for {scheduledVisitAt}.",
                "Визит назначен", "Визит по заявке {requestNumber} назначен на {scheduledVisitAt}.",
                "Tashrif belgilandi", "{requestNumber} arizasi bo'yicha tashrif {scheduledVisitAt} vaqtiga belgilandi.");
        put(NotificationType.VISIT_RESCHEDULED, NotificationRecipientType.CUSTOMER,
                "Visit rescheduled", "Visit for request {requestNumber} was moved to {scheduledVisitAt}.",
                "Визит перенесён", "Визит по заявке {requestNumber} перенесён на {scheduledVisitAt}.",
                "Tashrif ko'chirildi", "{requestNumber} arizasi bo'yicha tashrif {scheduledVisitAt} vaqtiga ko'chirildi.");
        put(NotificationType.VISIT_RESCHEDULED, NotificationRecipientType.TECHNICIAN,
                "Visit rescheduled", "Visit for request {requestNumber} was moved to {scheduledVisitAt}.",
                "Визит перенесён", "Визит по заявке {requestNumber} перенесён на {scheduledVisitAt}.",
                "Tashrif ko'chirildi", "{requestNumber} arizasi bo'yicha tashrif {scheduledVisitAt} vaqtiga ko'chirildi.");
        put(NotificationType.VISIT_CANCELLED, NotificationRecipientType.CUSTOMER,
                "Visit cancelled", "Visit time for request {requestNumber} is no longer confirmed.",
                "Визит отменён", "Время визита по заявке {requestNumber} больше не подтверждено.",
                "Tashrif bekor qilindi", "{requestNumber} arizasi bo'yicha tashrif vaqti hozircha tasdiqlanmagan.");
        put(NotificationType.VISIT_CANCELLED, NotificationRecipientType.TECHNICIAN,
                "Visit cancelled", "Visit time for request {requestNumber} is no longer confirmed.",
                "Визит отменён", "Время визита по заявке {requestNumber} больше не подтверждено.",
                "Tashrif bekor qilindi", "{requestNumber} arizasi bo'yicha tashrif vaqti hozircha tasdiqlanmagan.");
        put(NotificationType.REPAIR_STARTED, NotificationRecipientType.CUSTOMER,
                "Repair started", "Repair request {requestNumber} is now {status}.",
                "Ремонт начат", "Ремонт по заявке {requestNumber} теперь {status}.",
                "Ta'mirlash boshlandi", "{requestNumber} arizasi bo'yicha ta'mirlash holati: {status}.");
        put(NotificationType.WAITING_FOR_PARTS, NotificationRecipientType.CUSTOMER,
                "Waiting for parts", "Repair request {requestNumber} is {status}. Required parts are being arranged.",
                "Ожидаются запчасти", "Ремонт по заявке {requestNumber}: {status}. Необходимые запчасти подготавливаются.",
                "Qismlar kutilmoqda", "{requestNumber} arizasi bo'yicha ta'mirlash {status}. Kerakli qismlar tayyorlanmoqda.");
        put(NotificationType.REPAIR_RESUMED, NotificationRecipientType.CUSTOMER,
                "Repair resumed", "Repair request {requestNumber} resumed and is {status}.",
                "Ремонт возобновлён", "Ремонт по заявке {requestNumber} возобновлён, статус: {status}.",
                "Ta'mirlash davom etdi", "{requestNumber} arizasi bo'yicha ta'mirlash davom ettirildi, holat: {status}.");
        put(NotificationType.REPAIR_COMPLETED, NotificationRecipientType.CUSTOMER,
                "Repair completed", "Repair request {requestNumber} has been completed.",
                "Ремонт завершён", "Ремонт по заявке {requestNumber} завершён.",
                "Ta'mirlash yakunlandi", "{requestNumber} raqamli ta'mirlash arizasi yakunlandi.");
        put(NotificationType.REQUEST_CANCELLED, NotificationRecipientType.CUSTOMER,
                "Request cancelled", "Repair request {requestNumber} has been cancelled.",
                "Заявка отменена", "Заявка {requestNumber} отменена.",
                "Ariza bekor qilindi", "{requestNumber} raqamli ariza bekor qilindi.");
        put(NotificationType.REQUEST_CANCELLED, NotificationRecipientType.TECHNICIAN,
                "Request cancelled", "Repair request {requestNumber} assigned to you has been cancelled.",
                "Заявка отменена", "Назначенная вам заявка {requestNumber} отменена.",
                "Ariza bekor qilindi", "Sizga biriktirilgan {requestNumber} arizasi bekor qilindi.");
    }

    private void put(
            NotificationType type,
            NotificationRecipientType recipientType,
            String enTitle,
            String enMessage,
            String ruTitle,
            String ruMessage,
            String uzTitle,
            String uzMessage) {
        Map<NotificationRecipientType, Map<LanguageCode, Template>> byRecipient =
                templates.computeIfAbsent(type, ignored -> new EnumMap<>(NotificationRecipientType.class));
        Map<LanguageCode, Template> byLanguage =
                byRecipient.computeIfAbsent(recipientType, ignored -> new EnumMap<>(LanguageCode.class));
        byLanguage.put(LanguageCode.EN, new Template(enTitle, enMessage));
        byLanguage.put(LanguageCode.RU, new Template(ruTitle, ruMessage));
        byLanguage.put(LanguageCode.UZ, new Template(uzTitle, uzMessage));
    }

    private record Template(String title, String message) {
    }

    public record RenderedNotification(LanguageCode language, String title, String message) {
    }
}
