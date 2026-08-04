package com.example.darks.repair_auto.identity.api.dto;

import com.example.darks.repair_auto.identity.domain.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserSummaryResponse summary(User user) {
        return new UserSummaryResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }

    public static UserDetailsResponse details(User user) {
        return new UserDetailsResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getLastLoginAt(),
                user.getCreatedAt());
    }
}
