package com.example.darks.repair_auto.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record PasswordChangeRequest(
        @NotBlank
        @JsonProperty("oldPassword")
        @JsonAlias({"currentPassword", "old_password"})
        @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
        String oldPassword,

        @NotBlank
        @JsonProperty("newPassword")
        @JsonAlias({"new_password"})
        @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
        String newPassword,

        @JsonProperty("confirmPassword")
        @JsonAlias({"confirm_password", "passwordConfirmation"})
        @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
        String confirmPassword
) {
    public PasswordChangeRequest {
        if (confirmPassword == null && newPassword != null) {
            confirmPassword = newPassword;
        }
    }
}
