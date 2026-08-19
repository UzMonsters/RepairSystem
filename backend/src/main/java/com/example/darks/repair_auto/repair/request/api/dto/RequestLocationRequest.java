package com.example.darks.repair_auto.repair.request.api.dto;

import com.example.darks.repair_auto.repair.request.domain.RequestLocationSource;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = false)
@Schema(description = "Repair request location snapshot payload")
public record RequestLocationRequest(
        @DecimalMin(value = "-90.0000000", message = "latitude must be between -90 and 90")
        @DecimalMax(value = "90.0000000", message = "latitude must be between -90 and 90")
        @Schema(description = "Geographical latitude (-90 to 90)", example = "41.3110810")
        BigDecimal latitude,

        @DecimalMin(value = "-180.0000000", message = "longitude must be between -180 and 180")
        @DecimalMax(value = "180.0000000", message = "longitude must be between -180 and 180")
        @Schema(description = "Geographical longitude (-180 to 180)", example = "69.2405620")
        BigDecimal longitude,

        @Size(max = 500, message = "address must not exceed 500 characters")
        @Schema(description = "Physical address or landmark description (max 500 characters)", example = "Tashkent, Uzbekistan")
        String address,

        @Schema(description = "Location acquisition source method", example = "DEVICE_GPS")
        RequestLocationSource source
) {
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    public boolean hasAddress() {
        return address != null && !address.trim().isEmpty();
    }

    public boolean isEmpty() {
        return latitude == null && longitude == null && (address == null || address.trim().isEmpty());
    }
}
