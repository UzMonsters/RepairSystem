package com.example.darks.repair_auto.notification.push.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Backend-recognized Firebase application identifier")
public enum PushFirebaseApp {
    @Schema(description = "Admin / Manager Web Firebase app")
    ADMIN_WEB,

    @Schema(description = "Customer Android Firebase app")
    CUSTOMER_ANDROID,

    @Schema(description = "Customer iOS Firebase app")
    CUSTOMER_IOS,

    @Schema(description = "Technician Android Firebase app")
    TECHNICIAN_ANDROID,

    @Schema(description = "Technician iOS Firebase app")
    TECHNICIAN_IOS
}
