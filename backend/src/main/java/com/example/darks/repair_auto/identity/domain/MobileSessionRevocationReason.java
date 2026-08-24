package com.example.darks.repair_auto.identity.domain;

public enum MobileSessionRevocationReason {
    LOGOUT,
    LOGOUT_ALL,
    REFRESH_REUSE_DETECTED,
    ACCOUNT_INACTIVE,
    IDENTITY_CHANGED,
    CREDENTIAL_CHANGED,
    AUTH_METHOD_UNLINKED,
    EXPIRED,
    ADMIN_REVOKED,
    USER_REVOKED
}
