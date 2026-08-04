package com.example.darks.repair_auto.telegram.technician.api.dto;

import java.time.OffsetDateTime;

public record TechnicianTelegramLinkResponse(
        String deepLink,
        OffsetDateTime expiresAt) {
}
