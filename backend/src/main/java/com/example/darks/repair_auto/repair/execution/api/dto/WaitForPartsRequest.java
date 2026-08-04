package com.example.darks.repair_auto.repair.execution.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WaitForPartsRequest(
        @NotBlank @Size(max = 1000) String reason) {
}
