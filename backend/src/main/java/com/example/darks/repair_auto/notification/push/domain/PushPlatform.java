package com.example.darks.repair_auto.notification.push.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Operating platform of the client device")
public enum PushPlatform {
    @Schema(description = "Web browser client")
    WEB,

    @Schema(description = "Android mobile client")
    ANDROID,

    @Schema(description = "iOS mobile client")
    IOS
}
