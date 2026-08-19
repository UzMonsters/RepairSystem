package com.example.darks.repair_auto.dashboard.api.dto;

public record DashboardStatusLabelResponse(
        String label,
        String labelEn,
        String labelRu,
        String labelUz,
        String en,
        String ru,
        String uz) {

    public DashboardStatusLabelResponse(String label, String labelEn, String labelRu, String labelUz) {
        this(label, labelEn, labelRu, labelUz, labelEn, labelRu, labelUz);
    }
}
