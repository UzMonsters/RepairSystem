package com.example.darks.repair_auto.identity.mobile.auth.dto;

import java.util.UUID;

public record PhoneOtpResponse(
        UUID challengeId,
        long expiresIn,
        long resendAvailableIn,
        String deliveryStatus,
        String debugCode
) {
}
