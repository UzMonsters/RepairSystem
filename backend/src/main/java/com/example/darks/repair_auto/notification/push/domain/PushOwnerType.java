package com.example.darks.repair_auto.notification.push.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Actor family owning a push notification endpoint")
public enum PushOwnerType {
    @Schema(description = "Staff user (Admin or Manager)")
    STAFF,

    @Schema(description = "Customer mobile actor")
    CUSTOMER,

    @Schema(description = "Technician mobile actor")
    TECHNICIAN
}
