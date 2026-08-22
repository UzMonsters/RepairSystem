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
                .replace("{clientRequestNumber}", clientRequestNumber(payload))
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
            return switchDefault("not scheduled yet", "пока не назначено", "hali belgilanmagan", language);
        }
        return telegramDateFormatter.format(Instant.parse(value));
    }

    private String clientRequestNumber(Map<String, String> payload) {
        String requestNumber = value(payload, "requestNumber");
        int lastDash = requestNumber.lastIndexOf('-');
        if (lastDash >= 0 && lastDash + 1 < requestNumber.length()) {
            return requestNumber.substring(lastDash + 1);
        }
        return requestNumber;
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
                "Request received", "Your repair request has been received.\nRequest number: {clientRequestNumber}\nStatus: {status}.",
                "Заявка принята", "Ваша заявка на ремонт принята.\nНомер заявки: {clientRequestNumber}\nСтатус: {status}.",
                "Ariza qabul qilindi", "Ta'mirlash arizangiz qabul qilindi.\nAriza raqami: {clientRequestNumber}\nHolat: {status}.");
        put(NotificationType.TECHNICIAN_ASSIGNED, NotificationRecipientType.CUSTOMER,
                "Technician assigned", "A technician has been assigned to your request.\nRequest number: {clientRequestNumber}\nTechnician: {technicianName}\nVisit time: {scheduledVisitAt}.",
                "Мастер назначен", "По вашей заявке назначен мастер.\nНомер заявки: {clientRequestNumber}\nМастер: {technicianName}\nВремя визита: {scheduledVisitAt}.",
                "Usta tayinlandi", "Arizangiz bo'yicha usta tayinlandi.\nAriza raqami: {clientRequestNumber}\nUsta: {technicianName}\nTashrif vaqti: {scheduledVisitAt}.");
        put(NotificationType.TECHNICIAN_ASSIGNED, NotificationRecipientType.TECHNICIAN,
                "New assignment", "A new repair request {requestNumber} was assigned to you. Priority: {priority}. Visit: {scheduledVisitAt}.",
                "Новое назначение", "Вам назначена новая заявка {requestNumber}. Приоритет: {priority}. Визит: {scheduledVisitAt}.",
                "Yangi topshiriq", "Sizga {requestNumber} raqamli yangi ariza biriktirildi. Muhimlik: {priority}. Tashrif: {scheduledVisitAt}.");
        put(NotificationType.TECHNICIAN_UNASSIGNED, NotificationRecipientType.CUSTOMER,
                "Technician assignment in progress", "We are assigning a technician to your request again.\nRequest number: {clientRequestNumber}.",
                "Мастер подбирается", "Мы заново подбираем мастера по вашей заявке.\nНомер заявки: {clientRequestNumber}.",
                "Usta qayta tayinlanmoqda", "Arizangiz bo'yicha usta qayta tayinlanmoqda.\nAriza raqami: {clientRequestNumber}.");
        put(NotificationType.TECHNICIAN_UNASSIGNED, NotificationRecipientType.TECHNICIAN,
                "Assignment removed", "You were removed from repair request {requestNumber}.",
                "Назначение снято", "Вы сняты с заявки {requestNumber}.",
                "Topshiriq olib tashlandi", "Siz {requestNumber} arizasidan olib tashlandingiz.");
        put(NotificationType.VISIT_SCHEDULED, NotificationRecipientType.CUSTOMER,
                "Visit scheduled", "A visit has been scheduled for your request.\nRequest number: {clientRequestNumber}\nVisit time: {scheduledVisitAt}.",
                "Визит назначен", "По вашей заявке назначен визит.\nНомер заявки: {clientRequestNumber}\nВремя визита: {scheduledVisitAt}.",
                "Tashrif belgilandi", "Arizangiz bo'yicha tashrif belgilandi.\nAriza raqami: {clientRequestNumber}\nTashrif vaqti: {scheduledVisitAt}.");
        put(NotificationType.VISIT_SCHEDULED, NotificationRecipientType.TECHNICIAN,
                "Visit scheduled", "Visit for request {requestNumber} is scheduled for {scheduledVisitAt}.",
                "Визит назначен", "Визит по заявке {requestNumber} назначен на {scheduledVisitAt}.",
                "Tashrif belgilandi", "{requestNumber} arizasi bo'yicha tashrif {scheduledVisitAt} vaqtiga belgilandi.");
        put(NotificationType.VISIT_RESCHEDULED, NotificationRecipientType.CUSTOMER,
                "Visit rescheduled", "The visit time for your request has changed.\nRequest number: {clientRequestNumber}\nNew visit time: {scheduledVisitAt}.",
                "Визит перенесён", "Время визита по вашей заявке изменено.\nНомер заявки: {clientRequestNumber}\nНовое время визита: {scheduledVisitAt}.",
                "Tashrif vaqti o'zgartirildi", "Arizangiz bo'yicha tashrif vaqti o'zgartirildi.\nAriza raqami: {clientRequestNumber}\nYangi tashrif vaqti: {scheduledVisitAt}.");
        put(NotificationType.VISIT_RESCHEDULED, NotificationRecipientType.TECHNICIAN,
                "Visit rescheduled", "Visit for request {requestNumber} was moved to {scheduledVisitAt}.",
                "Визит перенесён", "Визит по заявке {requestNumber} перенесён на {scheduledVisitAt}.",
                "Tashrif ko'chirildi", "{requestNumber} arizasi bo'yicha tashrif {scheduledVisitAt} vaqtiga ko'chirildi.");
        put(NotificationType.VISIT_CANCELLED, NotificationRecipientType.CUSTOMER,
                "Visit time pending", "The visit time for your request is not confirmed yet.\nRequest number: {clientRequestNumber}.",
                "Время визита уточняется", "Время визита по вашей заявке пока не подтверждено.\nНомер заявки: {clientRequestNumber}.",
                "Tashrif vaqti aniqlanmoqda", "Arizangiz bo'yicha tashrif vaqti hali tasdiqlanmagan.\nAriza raqami: {clientRequestNumber}.");
        put(NotificationType.VISIT_CANCELLED, NotificationRecipientType.TECHNICIAN,
                "Visit cancelled", "Visit time for request {requestNumber} is no longer confirmed.",
                "Визит отменён", "Время визита по заявке {requestNumber} больше не подтверждено.",
                "Tashrif bekor qilindi", "{requestNumber} arizasi bo'yicha tashrif vaqti hozircha tasdiqlanmagan.");
        put(NotificationType.REPAIR_STARTED, NotificationRecipientType.CUSTOMER,
                "Repair started", "Repair work on your request has started.\nRequest number: {clientRequestNumber}.",
                "Ремонт начат", "Работы по вашей заявке начались.\nНомер заявки: {clientRequestNumber}.",
                "Ta'mirlash boshlandi", "Arizangiz bo'yicha ta'mirlash ishlari boshlandi.\nAriza raqami: {clientRequestNumber}.");
        put(NotificationType.WAITING_FOR_PARTS, NotificationRecipientType.CUSTOMER,
                "Waiting for parts", "Repair work is waiting for the required parts.\nRequest number: {clientRequestNumber}.",
                "Ожидаются запчасти", "Ремонт ожидает необходимые запчасти.\nНомер заявки: {clientRequestNumber}.",
                "Qismlar kutilmoqda", "Ta'mirlash uchun kerakli qismlar kutilmoqda.\nAriza raqami: {clientRequestNumber}.");
        put(NotificationType.REPAIR_RESUMED, NotificationRecipientType.CUSTOMER,
                "Repair resumed", "Repair work on your request has resumed.\nRequest number: {clientRequestNumber}.",
                "Ремонт возобновлён", "Работы по вашей заявке возобновлены.\nНомер заявки: {clientRequestNumber}.",
                "Ta'mirlash davom ettirildi", "Arizangiz bo'yicha ta'mirlash ishlari davom ettirildi.\nAriza raqami: {clientRequestNumber}.");
        put(NotificationType.REPAIR_COMPLETED, NotificationRecipientType.CUSTOMER,
                "Repair completed", "Repair work on your request has been completed.\nRequest number: {clientRequestNumber}.",
                "Ремонт завершён", "Работы по вашей заявке завершены.\nНомер заявки: {clientRequestNumber}.",
                "Ta'mirlash yakunlandi", "Arizangiz bo'yicha ta'mirlash ishlari yakunlandi.\nAriza raqami: {clientRequestNumber}.");
        put(NotificationType.REQUEST_CANCELLED, NotificationRecipientType.CUSTOMER,
                "Request cancelled", "Your repair request has been cancelled.\nRequest number: {clientRequestNumber}.",
                "Заявка отменена", "Ваша заявка на ремонт отменена.\nНомер заявки: {clientRequestNumber}.",
                "Ariza bekor qilindi", "Ta'mirlash arizangiz bekor qilindi.\nAriza raqami: {clientRequestNumber}.");
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
