package com.example.darks.repair_auto.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @JsonProperty("email")
        @NotBlank @Email String email,
        @JsonProperty("password")
        @NotBlank
        @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
        String password,
        @JsonProperty("rememberMe")
        Boolean rememberMe
) {
    public LoginRequest(String email, String password) {
        this(email, password, false);
    }

    public boolean isRememberMe() {
        return Boolean.TRUE.equals(rememberMe);
    }
}
