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
    void customerAssignmentNotificationIncludesTechnicianAndRequestNumber() {
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

        assertThat(rendered.message())
                .contains("John Week")
                .contains("REP-2026-000002");
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
}
