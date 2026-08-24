package com.example.darks.repair_auto.identity.mobile.email.dto;

import java.util.UUID;

public record EmailVerificationResponse(
        UUID challengeId,
        long expiresIn,
        long resendAvailableIn,
        String deliveryStatus
) {
}
