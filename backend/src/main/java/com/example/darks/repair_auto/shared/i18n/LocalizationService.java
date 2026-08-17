package com.example.darks.repair_auto.shared.i18n;

import jakarta.servlet.http.HttpServletRequest;

public interface LocalizationService {

    String get(String key);

    String get(String key, Object... args);

    String get(String key, SupportedLanguage language, Object... args);

    String get(String key, HttpServletRequest request, Object... args);
}
