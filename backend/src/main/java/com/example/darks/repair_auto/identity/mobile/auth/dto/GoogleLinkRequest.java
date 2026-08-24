package com.example.darks.repair_auto.identity.mobile.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLinkRequest(
        @NotBlank String idToken
) {
}
