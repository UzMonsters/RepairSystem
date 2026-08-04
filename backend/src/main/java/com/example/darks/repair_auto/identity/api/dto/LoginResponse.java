package com.example.darks.repair_auto.identity.api.dto;

import com.example.darks.repair_auto.identity.api.dto.UserSummaryResponse;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn,
        UserSummaryResponse user
) {
}
