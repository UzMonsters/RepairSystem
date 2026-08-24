package com.example.darks.repair_auto.customer.api.dto;

import com.example.darks.repair_auto.customer.domain.Customer;

public final class CustomerMapper {

    private CustomerMapper() {
    }

    public static CustomerSummaryResponse summary(Customer customer) {
        return new CustomerSummaryResponse(
                customer.getId(),
                customer.getFullName(),
                customer.getPhone(),
                customer.getPhoneVerifiedAt() != null,
                customer.getEmail(),
                customer.getEmailVerifiedAt() != null,
                customer.getPreferredLanguage(),
                customer.getRegistrationSource(),
                customer.isActive(),
                customer.isTelegramLinked(),
                customer.getCreatedAt(),
                customer.getUpdatedAt());
    }

    public static CustomerDetailResponse details(Customer customer) {
        return new CustomerDetailResponse(
                customer.getId(),
                customer.getFullName(),
                customer.getPhone(),
                customer.getPhoneVerifiedAt() != null,
                customer.getEmail(),
                customer.getEmailVerifiedAt() != null,
                customer.getPreferredLanguage(),
                customer.getRegistrationSource(),
                customer.isActive(),
                customer.isTelegramLinked(),
                customer.getCreatedAt(),
                customer.getUpdatedAt());
    }
}
