package com.example.darks.repair_auto.settings.api;

import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.settings.api.dto.SystemSettingsResponse;
import com.example.darks.repair_auto.settings.api.dto.SystemSettingsUpdateRequest;
import com.example.darks.repair_auto.settings.api.dto.UserSettingsResponse;
import com.example.darks.repair_auto.settings.api.dto.UserSettingsUpdateRequest;
import com.example.darks.repair_auto.settings.application.SettingsService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/me")
    public UserSettingsResponse getMySettings(@AuthenticationPrincipal AuthenticatedUser user) {
        return settingsService.getUserSettings(user.id());
    }

    @PutMapping("/me")
    public UserSettingsResponse updateMySettings(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UserSettingsUpdateRequest request) {
        return settingsService.updateUserSettings(user.id(), request);
    }

    @GetMapping("/system")
    public SystemSettingsResponse getSystemSettings() {
        return settingsService.getSystemSettings();
    }

    @PutMapping("/system")
    public SystemSettingsResponse updateSystemSettings(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody SystemSettingsUpdateRequest request) {
        return settingsService.updateSystemSettings(user.id(), request);
    }
}
