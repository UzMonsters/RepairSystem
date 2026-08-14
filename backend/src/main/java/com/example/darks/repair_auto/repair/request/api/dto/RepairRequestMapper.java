package com.example.darks.repair_auto.repair.request.api.dto;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentMapper;
import com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment;
import com.example.darks.repair_auto.repair.execution.api.dto.RepairExecutionMapper;
import com.example.darks.repair_auto.repair.execution.domain.RepairExecution;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;

public final class RepairRequestMapper {

    private RepairRequestMapper() {
    }

    public static RepairRequestCreateResponse created(RepairRequest request) {
        return new RepairRequestCreateResponse(
                request.getId(),
                request.getRequestNumber(),
                request.getStatus(),
                request.getPriority(),
                request.getSource(),
                request.getCreatedAt());
    }

    public static RepairRequestSummaryResponse summary(RepairRequest request) {
        return new RepairRequestSummaryResponse(
                request.getId(),
                request.getRequestNumber(),
                request.getStatus(),
                request.getPriority(),
                request.getSource(),
                request.getDescription(),
                request.getAddress(),
                request.getCustomerPreferredVisitAt(),
                customer(request.getCustomer()),
                category(request.getCategory()),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }

    public static RepairRequestDetailResponse details(RepairRequest request) {
        return details(request, null, null);
    }

    public static RepairRequestDetailResponse details(RepairRequest request, RepairAssignment currentAssignment) {
        return details(request, currentAssignment, null);
    }

    public static RepairRequestDetailResponse details(
            RepairRequest request,
            RepairAssignment currentAssignment,
            RepairExecution execution) {
        return new RepairRequestDetailResponse(
                request.getId(),
                request.getRequestNumber(),
                request.getStatus(),
                request.getPriority(),
                request.getSource(),
                request.getDescription(),
                request.getAddress(),
                request.getLatitude(),
                request.getLongitude(),
                request.getCustomerPreferredVisitAt(),
                request.getInternalNote(),
                customer(request.getCustomer()),
                category(request.getCategory()),
                AssignmentMapper.current(currentAssignment),
                RepairExecutionMapper.summary(execution),
                user(request.getCreatedByUser()),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }

    private static RepairRequestCustomerSummary customer(Customer customer) {
        return new RepairRequestCustomerSummary(
                customer.getId(),
                customer.getFullName(),
                customer.getPhone(),
                customer.getPreferredLanguage(),
                customer.isActive());
    }

    private static RepairRequestCategorySummary category(RepairCategory category) {
        if (category == null) {
            return null;
        }
        String name = category.getNameUz() != null ? category.getNameUz() : (category.getNameRu() != null ? category.getNameRu() : category.getNameEn());
        String description = category.getDescriptionUz() != null ? category.getDescriptionUz() : (category.getDescriptionRu() != null ? category.getDescriptionRu() : category.getDescriptionEn());
        return new RepairRequestCategorySummary(
                category.getId(),
                name,
                description,
                category.getNameEn(),
                category.getNameRu(),
                category.getNameUz(),
                category.isActive());
    }

    private static RepairRequestUserSummary user(User user) {
        if (user == null) {
            return null;
        }
        return new RepairRequestUserSummary(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}
