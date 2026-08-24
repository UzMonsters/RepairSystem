package com.example.darks.repair_auto.identity.mobile.auth.dto;

import com.example.darks.repair_auto.identity.domain.MobileAuthProvider;
import java.time.OffsetDateTime;

public record MobileAuthMethodResponse(
        MobileAuthProvider provider,
        boolean linked,
        String displayValue,
        OffsetDateTime linkedAt,
        OffsetDateTime lastUsedAt
) {
}
