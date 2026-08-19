package com.example.darks.repair_auto.repair.request.api.dto;

import com.example.darks.repair_auto.repair.request.domain.RequestLocationSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Repair request location snapshot response")
public record RequestLocationResponse(
        @Schema(description = "Geographical latitude", example = "41.3110810")
        BigDecimal latitude,

        @Schema(description = "Geographical longitude", example = "69.2405620")
        BigDecimal longitude,

        @Schema(description = "Physical address or landmark", example = "Tashkent, Uzbekistan")
        String address,

        @Schema(description = "Location acquisition source method", example = "DEVICE_GPS")
        RequestLocationSource source
) {
}
