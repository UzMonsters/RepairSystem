package com.example.darks.repair_auto.notification.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NotificationRetryRequest(
        @NotBlank
        @Size(max = 500)
        String reason) {
}
