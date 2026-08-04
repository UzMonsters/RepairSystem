package com.example.darks.repair_auto.dashboard.api.dto;

public record TechnicianDashboardResponse(
        long activeTechnicians,
        long inactiveTechnicians,
        long techniciansWithActiveWork,
        long techniciansWithoutActiveWork,
        long pendingAssignments,
        long acceptedAssignments,
        long inProgressRequests,
        long waitingForPartsRequests,
        long availableCapacity,
        long totalCapacity) {
}
