package com.example.darks.repair_auto.notification.push.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "RepairAuto application client type registering the push endpoint")
public enum PushClientType {
    @Schema(description = "Admin / Manager Web portal")
    ADMIN_WEB,

    @Schema(description = "Customer mobile application")
    CUSTOMER_MOBILE,

    @Schema(description = "Technician mobile application")
    TECHNICIAN_MOBILE
}
