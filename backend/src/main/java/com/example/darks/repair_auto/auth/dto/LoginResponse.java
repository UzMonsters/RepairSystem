package com.example.darks.repair_auto.auth.dto;

import com.example.darks.repair_auto.user.dto.UserSummaryResponse;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn,
        UserSummaryResponse user
) {
}
