package com.example.darks.repair_auto.settings.application;

import com.example.darks.repair_auto.settings.api.dto.SystemSettingsResponse;
import com.example.darks.repair_auto.settings.api.dto.SystemSettingsUpdateRequest;
import com.example.darks.repair_auto.settings.api.dto.UserSettingsResponse;
import com.example.darks.repair_auto.settings.api.dto.UserSettingsUpdateRequest;
import com.example.darks.repair_auto.settings.domain.SystemSettings;
import com.example.darks.repair_auto.settings.domain.UserSettings;
import com.example.darks.repair_auto.settings.infrastructure.SystemSettingsRepository;
import com.example.darks.repair_auto.settings.infrastructure.UserSettingsRepository;
import com.example.darks.repair_auto.shared.error.InvalidRequestParameterException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

    private final UserSettingsRepository userSettingsRepository;
    private final SystemSettingsRepository systemSettingsRepository;
    private final Clock clock;

    public SettingsService(
            UserSettingsRepository userSettingsRepository,
            SystemSettingsRepository systemSettingsRepository,
            Clock clock) {
        this.userSettingsRepository = userSettingsRepository;
        this.systemSettingsRepository = systemSettingsRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public UserSettingsResponse getUserSettings(Long userId) {
        return userSettingsRepository.findByUserId(userId)
                .map(this::toUserSettingsResponse)
                .orElseGet(UserSettingsResponse::defaults);
    }

    @Transactional
    public UserSettingsResponse updateUserSettings(Long userId, UserSettingsUpdateRequest request) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> new UserSettings(userId, request.language(), request.dateFormat(), request.timeFormat(), request.theme(), now));

        settings.setLanguage(request.language(), now);
        settings.setDateFormat(request.dateFormat(), now);
        settings.setTimeFormat(request.timeFormat(), now);
        settings.setTheme(request.theme(), now);

        UserSettings saved = userSettingsRepository.save(settings);
        return toUserSettingsResponse(saved);
    }

    @Transactional(readOnly = true)
    public SystemSettingsResponse getSystemSettings() {
        return systemSettingsRepository.findById(1L)
                .map(this::toSystemSettingsResponse)
                .orElseGet(SystemSettingsResponse::defaults);
    }

    @Transactional
    public SystemSettingsResponse updateSystemSettings(Long updatedByUserId, SystemSettingsUpdateRequest request) {
        validateTimezone(request.timezone());
        OffsetDateTime now = OffsetDateTime.now(clock);

        SystemSettings settings = systemSettingsRepository.findById(1L)
                .orElseGet(() -> new SystemSettings(1L, request.timezone(), request.defaultLanguage(), now, updatedByUserId));

        settings.setTimezone(request.timezone(), now, updatedByUserId);
        settings.setDefaultLanguage(request.defaultLanguage(), now, updatedByUserId);

        SystemSettings saved = systemSettingsRepository.save(settings);
        return toSystemSettingsResponse(saved);
    }

    private void validateTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            throw new InvalidRequestParameterException("timezone", "Timezone cannot be blank.");
        }
        try {
            ZoneId.of(timezone.trim());
        } catch (Exception e) {
            throw new InvalidRequestParameterException("timezone", "Invalid timezone: " + timezone);
        }
    }

    private UserSettingsResponse toUserSettingsResponse(UserSettings settings) {
        return new UserSettingsResponse(
                settings.getLanguage(),
                settings.getDateFormat(),
                settings.getTimeFormat(),
                settings.getTheme()
        );
    }

    private SystemSettingsResponse toSystemSettingsResponse(SystemSettings settings) {
        return new SystemSettingsResponse(
                settings.getTimezone(),
                settings.getDefaultLanguage()
        );
    }
}
