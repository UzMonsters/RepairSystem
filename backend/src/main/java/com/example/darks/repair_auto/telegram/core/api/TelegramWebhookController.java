package com.example.darks.repair_auto.telegram.core.api;

import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.telegram.core.application.TelegramWebhookService;
import com.example.darks.repair_auto.telegram.core.infrastructure.TelegramProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/telegram")
public class TelegramWebhookController {

    private static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final TelegramWebhookService webhookService;
    private final TelegramProperties properties;

    public TelegramWebhookController(TelegramWebhookService webhookService, TelegramProperties properties) {
        this.webhookService = webhookService;
        this.properties = properties;
    }

    @PostMapping(path = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> webhook(
            @RequestHeader(value = SECRET_HEADER, required = false) String secret,
            @RequestBody String rawBody) {
        if (!properties.isEnabled()) {
            throw new BusinessRuleException("TELEGRAM_DISABLED", "Telegram bot is disabled.", 404);
        }
        validateSecret(secret);
        if (rawBody == null
                || rawBody.getBytes(StandardCharsets.UTF_8).length > properties.getMaxWebhookBodySize()) {
            throw new BusinessRuleException("TELEGRAM_UPDATE_INVALID", "Telegram update is invalid.", 400);
        }
        webhookService.process(rawBody);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    private void validateSecret(String provided) {
        byte[] expected = properties.getWebhookSecret().getBytes(StandardCharsets.UTF_8);
        byte[] actual = provided == null ? new byte[0] : provided.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new BusinessRuleException(
                    "TELEGRAM_WEBHOOK_UNAUTHORIZED",
                    "Telegram webhook secret is invalid.",
                    401);
        }
    }
}
