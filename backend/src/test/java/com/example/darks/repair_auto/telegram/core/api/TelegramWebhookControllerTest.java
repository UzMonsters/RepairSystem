package com.example.darks.repair_auto.telegram.core.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.telegram.core.application.TelegramWebhookService;
import com.example.darks.repair_auto.telegram.core.domain.TelegramUserMode;
import com.example.darks.repair_auto.telegram.core.infrastructure.TelegramProperties;
import org.junit.jupiter.api.Test;

class TelegramWebhookControllerTest {

    @Test
    void givenTechnicianEndpointWhenCustomerSecretProvidedThenRejectsWithoutProcessing() {
        TelegramWebhookService webhookService = mock(TelegramWebhookService.class);
        TelegramWebhookController controller = new TelegramWebhookController(webhookService, properties());

        assertThatThrownBy(() -> controller.technicianWebhook("customer-secret", "{}"))
                .isInstanceOfSatisfying(BusinessRuleException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("TELEGRAM_WEBHOOK_UNAUTHORIZED");
                    assertThat(exception.status()).isEqualTo(401);
                });

        verify(webhookService, never()).process("{}", TelegramUserMode.TECHNICIAN);
    }

    @Test
    void givenTechnicianEndpointWhenTechnicianSecretProvidedThenProcessesTechnicianMode() {
        TelegramWebhookService webhookService = mock(TelegramWebhookService.class);
        TelegramWebhookController controller = new TelegramWebhookController(webhookService, properties());

        var response = controller.technicianWebhook("technician-secret", "{}");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(webhookService).process("{}", TelegramUserMode.TECHNICIAN);
    }

    @Test
    void givenCustomerEndpointWhenCustomerSecretProvidedThenProcessesCustomerMode() {
        TelegramWebhookService webhookService = mock(TelegramWebhookService.class);
        TelegramWebhookController controller = new TelegramWebhookController(webhookService, properties());

        var response = controller.customerWebhook("customer-secret", "{}");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(webhookService).process("{}", TelegramUserMode.CUSTOMER);
    }

    private TelegramProperties properties() {
        TelegramProperties properties = new TelegramProperties();
        properties.setEnabled(true);
        properties.getCustomer().setWebhookSecret("customer-secret");
        properties.getTechnician().setWebhookSecret("technician-secret");
        return properties;
    }
}
