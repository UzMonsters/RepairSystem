package com.example.darks.repair_auto.repair.request.mobile.api.dto;

import com.example.darks.repair_auto.repair.request.api.dto.RequestLocationRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Customer repair request creation payload")
public record CustomerRepairRequestCreateRequest(
        @NotNull
        @Schema(description = "Repair category ID", example = "4")
        Long categoryId,

        @NotBlank
        @Size(min = 10, max = 2000)
        @Schema(description = "Description of the problem", example = "Air conditioner is making noise and not cooling")
        String description,

        @Valid
        @Schema(description = "Location snapshot information")
        RequestLocationRequest location,

        @Size(max = 500, message = "{repair.request.location-address-too-long}")
        @Schema(description = "Physical address or landmark", example = "Chilanzar 9, Tashkent")
        String address,

        @DecimalMin(value = "-90.000000", message = "{repair.request.location-latitude-invalid}")
        @DecimalMax(value = "90.000000", message = "{repair.request.location-latitude-invalid}")
        @Schema(description = "Geographical latitude", example = "41.275412")
        BigDecimal latitude,

        @DecimalMin(value = "-180.000000", message = "{repair.request.location-longitude-invalid}")
        @DecimalMax(value = "180.000000", message = "{repair.request.location-longitude-invalid}")
        @Schema(description = "Geographical longitude", example = "69.204511")
        BigDecimal longitude
) {
    public CustomerRepairRequestCreateRequest(
            Long categoryId,
            String description,
            String address,
            BigDecimal latitude,
            BigDecimal longitude) {
        this(
                categoryId,
                description,
                null,
                address,
                latitude,
                longitude);
    }

    public RequestLocationRequest resolvedLocation() {
        if (location != null) {
            return location;
        }
        if (address != null || latitude != null || longitude != null) {
            return new RequestLocationRequest(latitude, longitude, address, null);
        }
        return null;
    }
}
