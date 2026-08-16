package com.example.darks.repair_auto.identity.api.dto;

import com.example.darks.repair_auto.identity.api.dto.UserSummaryResponse;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn,
        boolean rememberMe,
        UserSummaryResponse user
) {
    public LoginResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long accessTokenExpiresIn,
            long refreshTokenExpiresIn,
            UserSummaryResponse user) {
        this(accessToken, refreshToken, tokenType, accessTokenExpiresIn, refreshTokenExpiresIn, false, user);
    }
}
