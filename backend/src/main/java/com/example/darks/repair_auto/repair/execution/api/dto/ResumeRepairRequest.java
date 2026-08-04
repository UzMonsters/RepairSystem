package com.example.darks.repair_auto.repair.execution.api.dto;

import jakarta.validation.constraints.Size;

public record ResumeRepairRequest(
        @Size(max = 1000) String note) {
}
