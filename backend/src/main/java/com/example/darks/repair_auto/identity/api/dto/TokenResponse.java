package com.example.darks.repair_auto.identity.api.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn,
        boolean rememberMe
) {
    public TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long accessTokenExpiresIn,
            long refreshTokenExpiresIn) {
        this(accessToken, refreshToken, tokenType, accessTokenExpiresIn, refreshTokenExpiresIn, false);
    }
}
