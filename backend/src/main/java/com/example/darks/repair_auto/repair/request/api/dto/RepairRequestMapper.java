package com.example.darks.repair_auto.repair.request.api.dto;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentMapper;
import com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment;
import com.example.darks.repair_auto.repair.execution.api.dto.RepairExecutionMapper;
import com.example.darks.repair_auto.repair.execution.domain.RepairExecution;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;

import com.example.darks.repair_auto.localization.application.LocalizedValueResolver;
import com.example.darks.repair_auto.settings.domain.Language;

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
        return summary(request, null, null);
    }

    public static RepairRequestSummaryResponse summary(
            RepairRequest request,
            Language language,
            LocalizedValueResolver resolver) {
        return new RepairRequestSummaryResponse(
                request.getId(),
                request.getRequestNumber(),
                request.getStatus(),
                request.getPriority(),
                request.getSource(),
                request.getDescription(),
                request.getAddress(),
                request.getCustomerPreferredVisitAt(),
                request.getCustomer() != null ? request.getCustomer().getFullName() : null,
                category(request.getCategory(), language, resolver),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }

    public static RepairRequestDetailResponse details(RepairRequest request) {
        return details(request, null, null, null, null);
    }

    public static RepairRequestDetailResponse details(RepairRequest request, RepairAssignment currentAssignment) {
        return details(request, currentAssignment, null, null, null);
    }

    public static RepairRequestDetailResponse details(
            RepairRequest request,
            RepairAssignment currentAssignment,
            RepairExecution execution) {
        return details(request, currentAssignment, execution, null, null);
    }

    public static RepairRequestDetailResponse details(
            RepairRequest request,
            RepairAssignment currentAssignment,
            RepairExecution execution,
            Language language,
            LocalizedValueResolver resolver) {
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
                category(request.getCategory(), language, resolver),
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

    private static RepairRequestCategorySummary category(
            RepairCategory category,
            Language language,
            LocalizedValueResolver resolver) {
        if (category == null) {
            return null;
        }
        String name = resolver != null
                ? resolver.resolve(language, category.getNameUz(), category.getNameRu(), category.getNameEn())
                : (category.getNameUz() != null ? category.getNameUz() : (category.getNameRu() != null ? category.getNameRu() : category.getNameEn()));
        String description = resolver != null
                ? resolver.resolve(language, category.getDescriptionUz(), category.getDescriptionRu(), category.getDescriptionEn())
                : (category.getDescriptionUz() != null ? category.getDescriptionUz() : (category.getDescriptionRu() != null ? category.getDescriptionRu() : category.getDescriptionEn()));
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
