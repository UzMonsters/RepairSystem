package com.example.darks.repair_auto.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.settings.api.dto.SystemSettingsResponse;
import com.example.darks.repair_auto.settings.api.dto.SystemSettingsUpdateRequest;
import com.example.darks.repair_auto.settings.api.dto.UserSettingsResponse;
import com.example.darks.repair_auto.settings.api.dto.UserSettingsUpdateRequest;
import com.example.darks.repair_auto.settings.application.SettingsService;
import com.example.darks.repair_auto.settings.domain.DateFormat;
import com.example.darks.repair_auto.settings.domain.Language;
import com.example.darks.repair_auto.settings.domain.SystemSettings;
import com.example.darks.repair_auto.settings.domain.Theme;
import com.example.darks.repair_auto.settings.domain.TimeFormat;
import com.example.darks.repair_auto.settings.domain.UserSettings;
import com.example.darks.repair_auto.settings.infrastructure.SystemSettingsRepository;
import com.example.darks.repair_auto.settings.infrastructure.UserSettingsRepository;
import com.example.darks.repair_auto.shared.error.InvalidRequestParameterException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private SystemSettingsRepository systemSettingsRepository;

    private Clock clock;
    private SettingsService settingsService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-08-14T10:00:00Z"), ZoneOffset.UTC);
        settingsService = new SettingsService(userSettingsRepository, systemSettingsRepository, clock);
    }

    @Test
    void getUserSettings_whenNoSettingsFound_returnsDefaults() {
        when(userSettingsRepository.findByUserId(10L)).thenReturn(Optional.empty());

        UserSettingsResponse response = settingsService.getUserSettings(10L);

        assertThat(response.language()).isEqualTo(Language.UZ);
        assertThat(response.dateFormat()).isEqualTo(DateFormat.DD_MM_YYYY);
        assertThat(response.timeFormat()).isEqualTo(TimeFormat.HOUR_24);
        assertThat(response.theme()).isEqualTo(Theme.SYSTEM);
    }

    @Test
    void getUserSettings_whenSettingsExist_returnsSavedSettings() {
        UserSettings userSettings = new UserSettings(
                10L,
                Language.RU,
                DateFormat.YYYY_MM_DD,
                TimeFormat.HOUR_12,
                Theme.DARK,
                OffsetDateTime.now(clock));
        when(userSettingsRepository.findByUserId(10L)).thenReturn(Optional.of(userSettings));

        UserSettingsResponse response = settingsService.getUserSettings(10L);

        assertThat(response.language()).isEqualTo(Language.RU);
        assertThat(response.dateFormat()).isEqualTo(DateFormat.YYYY_MM_DD);
        assertThat(response.timeFormat()).isEqualTo(TimeFormat.HOUR_12);
        assertThat(response.theme()).isEqualTo(Theme.DARK);
    }

    @Test
    void updateUserSettings_savesAndReturnsUpdatedSettings() {
        UserSettingsUpdateRequest request = new UserSettingsUpdateRequest(
                Language.EN,
                DateFormat.DD_SLASH_MM_SLASH_YYYY,
                TimeFormat.HOUR_24,
                Theme.LIGHT);
        when(userSettingsRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSettingsResponse response = settingsService.updateUserSettings(10L, request);

        assertThat(response.language()).isEqualTo(Language.EN);
        assertThat(response.dateFormat()).isEqualTo(DateFormat.DD_SLASH_MM_SLASH_YYYY);
        assertThat(response.timeFormat()).isEqualTo(TimeFormat.HOUR_24);
        assertThat(response.theme()).isEqualTo(Theme.LIGHT);
        verify(userSettingsRepository).save(any(UserSettings.class));
    }

    @Test
    void getSystemSettings_whenNoSettingsFound_returnsDefaults() {
        when(systemSettingsRepository.findById(1L)).thenReturn(Optional.empty());

        SystemSettingsResponse response = settingsService.getSystemSettings();

        assertThat(response.timezone()).isEqualTo("Asia/Tashkent");
        assertThat(response.defaultLanguage()).isEqualTo(Language.UZ);
    }

    @Test
    void updateSystemSettings_whenValidTimezone_savesSettings() {
        SystemSettingsUpdateRequest request = new SystemSettingsUpdateRequest("Asia/Tashkent", Language.RU);
        when(systemSettingsRepository.findById(1L)).thenReturn(Optional.empty());
        when(systemSettingsRepository.save(any(SystemSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SystemSettingsResponse response = settingsService.updateSystemSettings(1L, request);

        assertThat(response.timezone()).isEqualTo("Asia/Tashkent");
        assertThat(response.defaultLanguage()).isEqualTo(Language.RU);
        verify(systemSettingsRepository).save(any(SystemSettings.class));
    }

    @Test
    void updateSystemSettings_whenInvalidTimezone_throwsException() {
        SystemSettingsUpdateRequest request = new SystemSettingsUpdateRequest("Invalid/Timezone", Language.UZ);

        assertThatThrownBy(() -> settingsService.updateSystemSettings(1L, request))
                .isInstanceOf(InvalidRequestParameterException.class);
    }
}
