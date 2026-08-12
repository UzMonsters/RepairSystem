package com.example.darks.repair_auto.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darks.repair_auto.notification.application.NotificationTemplateService;
import com.example.darks.repair_auto.notification.domain.NotificationType;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class NotificationTemplateServiceTest {

    private final NotificationTemplateService service = new NotificationTemplateService(ZoneId.of("Asia/Tashkent"));

    @Test
    void customerAssignmentNotificationDoesNotExposeRequestCode() {
        String text = service.render(
                NotificationType.CUSTOMER_TECHNICIAN_ASSIGNED,
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

        assertThat(text)
                .contains("John Week")
                .doesNotContain("REP-2026-000002");
    }

    @Test
    void technicianAssignmentNotificationPointsToExistingPendingMenu() {
        String text = service.render(
                NotificationType.TECHNICIAN_NEW_ASSIGNMENT,
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

        assertThat(text)
                .contains("Kutilayotgan")
                .doesNotContain("Mening ishlarim")
                .doesNotContain("REP-2026-000002");
    }
}
