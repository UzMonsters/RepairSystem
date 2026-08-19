package com.example.darks.repair_auto.repair.request.mobile.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Customer repair request creation payload")
public record CustomerRepairRequestCreateRequest(
        @NotNull(message = "categoryId is required")
        @Schema(description = "Repair category ID", example = "4")
        Long categoryId,

        @NotBlank(message = "description is required")
        @Size(min = 10, max = 2000, message = "description must be between 10 and 2000 characters")
        @Schema(description = "Description of the problem", example = "Air conditioner is making noise and not cooling")
        String description,

        @Size(max = 500, message = "address must not exceed 500 characters")
        @Schema(description = "Physical address or landmark", example = "Chilanzar 9, Tashkent")
        String address,

        @DecimalMin(value = "-90.000000", message = "latitude must be between -90 and 90")
        @DecimalMax(value = "90.000000", message = "latitude must be between -90 and 90")
        @Schema(description = "Geographical latitude", example = "41.275412")
        BigDecimal latitude,

        @DecimalMin(value = "-180.000000", message = "longitude must be between -180 and 180")
        @DecimalMax(value = "180.000000", message = "longitude must be between -180 and 180")
        @Schema(description = "Geographical longitude", example = "69.204511")
        BigDecimal longitude
) {
}
