package com.example.darks.repair_auto.repair.assignment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignmentRejectionRequest(
        @NotBlank @Size(max = 500) String reason) {
}
