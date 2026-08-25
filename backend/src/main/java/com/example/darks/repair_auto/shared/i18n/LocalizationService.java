package com.example.darks.repair_auto.shared.i18n;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;

public interface LocalizationService {

    String get(String key);

    String get(String key, Object... args);

    String get(String key, SupportedLanguage language, Object... args);

    default String get(String key, Locale locale, Object... args) {
        SupportedLanguage language = locale != null ? SupportedLanguage.fromCode(locale.getLanguage()) : null;
        return get(key, language, args);
    }

    String get(String key, HttpServletRequest request, Object... args);
}
