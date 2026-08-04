package com.example.darks.repair_auto.repair.execution.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteRepairRequest(
        @NotBlank @Size(max = 4000) String workPerformed,
        @Size(max = 2000) String completionNote) {
}
