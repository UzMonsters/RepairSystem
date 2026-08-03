package com.example.darks.repair_auto.repair.execution.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiagnosisRequest(
        @NotBlank @Size(max = 4000) String diagnosis) {
}
