package com.example.darks.repair_auto.repair.technician.mobile.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "View filter for technician job list")
public enum TechnicianJobListView {
    ACTIVE,
    HISTORY
}
