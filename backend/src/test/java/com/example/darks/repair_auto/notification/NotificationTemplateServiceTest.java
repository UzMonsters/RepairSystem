package com.example.darks.repair_auto.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darks.repair_auto.notification.application.NotificationTemplateService;
import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
import com.example.darks.repair_auto.notification.domain.NotificationType;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class NotificationTemplateServiceTest {

    private final NotificationTemplateService service = new NotificationTemplateService(ZoneId.of("Asia/Tashkent"));

    @Test
    void customerAssignmentNotificationIncludesTechnicianAndClientFriendlyRequestNumber() {
        var rendered = service.render(
                NotificationType.TECHNICIAN_ASSIGNED,
                NotificationRecipientType.CUSTOMER,
                """
                        {
                          "requestNumber":"REP-2026-000002",
                          "categoryNameEn":"Washer",
                          "categoryNameRu":"Стиральная машина",
                          "categoryNameUz":"Kir yuvish mashinasi",
                          "priority":"NORMAL",
                          "status":"ASSIGNED",
                          "technicianName":"John Week"
                        }
                        """,
                1,
                LanguageCode.RU);

        assertThat(rendered.title()).isEqualTo("Мастер назначен");
        assertThat(rendered.message())
                .contains("John Week")
                .contains("Номер заявки: 000002")
                .doesNotContain("REP-2026-000002");
    }

    @Test
    void technicianAssignmentNotificationUsesTechnicianVariant() {
        var rendered = service.render(
                NotificationType.TECHNICIAN_ASSIGNED,
                NotificationRecipientType.TECHNICIAN,
                """
                        {
                          "requestNumber":"REP-2026-000002",
                          "categoryNameEn":"Washer",
                          "categoryNameRu":"Стиральная машина",
                          "categoryNameUz":"Kir yuvish mashinasi",
                          "priority":"NORMAL",
                          "status":"ASSIGNED"
                        }
                        """,
                1,
                LanguageCode.UZ);

        assertThat(rendered.title()).isEqualTo("Yangi topshiriq");
        assertThat(rendered.message()).contains("REP-2026-000002");
    }

    @Test
    void customerNotificationsUseClientCopyInAllLanguages() {
        String payload = """
                {
                  "requestNumber":"REP-2026-000009",
                  "categoryNameEn":"Washer",
                  "categoryNameRu":"Стиральная машина",
                  "categoryNameUz":"Kir yuvish mashinasi",
                  "priority":"NORMAL",
                  "status":"IN_PROGRESS",
                  "technicianName":"Aminjon Samiyev"
                }
                """;

        var en = service.render(NotificationType.REPAIR_STARTED, NotificationRecipientType.CUSTOMER, payload, 1, LanguageCode.EN);
        var ru = service.render(NotificationType.REPAIR_STARTED, NotificationRecipientType.CUSTOMER, payload, 1, LanguageCode.RU);
        var uz = service.render(NotificationType.REPAIR_STARTED, NotificationRecipientType.CUSTOMER, payload, 1, LanguageCode.UZ);

        assertThat(en.message()).contains("Repair work on your request has started.", "Request number: 000009");
        assertThat(ru.message()).contains("Работы по вашей заявке начались.", "Номер заявки: 000009");
        assertThat(uz.message()).contains("Arizangiz bo'yicha ta'mirlash ishlari boshlandi.", "Ariza raqami: 000009");
        assertThat(en.message() + ru.message() + uz.message()).doesNotContain("REP-2026-000009");
    }

    @Test
    void customerUnassignedNotificationIsSoftened() {
        var rendered = service.render(
                NotificationType.TECHNICIAN_UNASSIGNED,
                NotificationRecipientType.CUSTOMER,
                """
                        {
                          "requestNumber":"REP-2026-000009",
                          "categoryNameEn":"Washer",
                          "categoryNameRu":"Стиральная машина",
                          "categoryNameUz":"Kir yuvish mashinasi",
                          "priority":"NORMAL",
                          "status":"ASSIGNED"
                        }
                        """,
                1,
                LanguageCode.UZ);

        assertThat(rendered.title()).isEqualTo("Usta qayta tayinlanmoqda");
        assertThat(rendered.message())
                .contains("Arizangiz bo'yicha usta qayta tayinlanmoqda.")
                .doesNotContain("olib tashlandi");
    }

    @Test
    void staffRejectionNotificationIncludesTechnicianRequestNumberReasonInAllLanguages() {
        String payload = """
                {
                  "requestId":"2",
                  "requestNumber":"REP-2026-000002",
                  "technicianId":"5",
                  "technicianName":"John Week",
                  "priority":"HIGH",
                  "status":"NEW",
                  "reason":"Too busy today"
                }
                """;

        var en = service.render(NotificationType.TECHNICIAN_REJECTED, NotificationRecipientType.STAFF, payload, 1, LanguageCode.EN);
        var ru = service.render(NotificationType.TECHNICIAN_REJECTED, NotificationRecipientType.STAFF, payload, 1, LanguageCode.RU);
        var uz = service.render(NotificationType.TECHNICIAN_REJECTED, NotificationRecipientType.STAFF, payload, 1, LanguageCode.UZ);

        assertThat(en.title()).isEqualTo("Technician rejected assignment");
        assertThat(en.message())
                .contains("Technician John Week rejected assignment for request REP-2026-000002.")
                .contains("Reassignment is required.")
                .contains("Reason: Too busy today.");

        assertThat(ru.title()).isEqualTo("Мастер отклонил назначение");
        assertThat(ru.message())
                .contains("Мастер John Week отклонил заявку REP-2026-000002.")
                .contains("Требуется переназначение.")
                .contains("Причина: Too busy today.");

        assertThat(uz.title()).isEqualTo("Usta topshiriqni rad etdi");
        assertThat(uz.message())
                .contains("Usta John Week REP-2026-000002 arizasini rad etdi.")
                .contains("Qayta biriktirish talab qilinadi.")
                .contains("Sabab: Too busy today.");
    }
}
