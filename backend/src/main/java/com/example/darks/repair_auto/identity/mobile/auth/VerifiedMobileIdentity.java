package com.example.darks.repair_auto.identity.mobile.auth;

import com.example.darks.repair_auto.identity.domain.MobileAuthProvider;

public record VerifiedMobileIdentity(
        MobileAuthProvider provider,
        String providerSubject,
        String email,
        boolean emailVerified,
        String phone,
        String displayName
) {
}
