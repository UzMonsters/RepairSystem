package com.example.darks.repair_auto.identity.domain;

public enum MobileRefreshRevocationReason {
    ROTATED,
    LOGOUT,
    LOGOUT_ALL,
    REUSE_DETECTED,
    ACCOUNT_INACTIVE,
    TELEGRAM_IDENTITY_CHANGED,
    CREDENTIAL_CHANGED,
    AUTH_METHOD_UNLINKED,
    IDENTITY_CHANGED
}
