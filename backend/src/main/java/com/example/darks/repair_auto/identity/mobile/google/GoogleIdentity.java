package com.example.darks.repair_auto.identity.mobile.google;

public record GoogleIdentity(
        String subject,
        String email,
        boolean emailVerified,
        String name
) {
}
