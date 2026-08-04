package com.example.darks.repair_auto.repair.assignment.api.dto;

public record TechnicianWorkloadResponse(
        Long technicianId,
        boolean active,
        int maximumConcurrentRequests,
        long pendingAssignments,
        long acceptedAssignments,
        long totalActiveAssignments,
        long remainingCapacity,
        boolean available) {
}
