package com.example.darks.repair_auto.shared.i18n;

import com.example.darks.repair_auto.settings.domain.Language;

public enum SupportedLanguage {
    UZ("uz"),
    RU("ru"),
    EN("en");

    private final String code;

    SupportedLanguage(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static SupportedLanguage fromCode(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.trim().toLowerCase();
        for (SupportedLanguage lang : values()) {
            if (lang.code.equalsIgnoreCase(normalized) || lang.name().equalsIgnoreCase(normalized)) {
                return lang;
            }
        }
        if (normalized.startsWith("uz")) return UZ;
        if (normalized.startsWith("ru")) return RU;
        if (normalized.startsWith("en")) return EN;
        return null;
    }

    public static SupportedLanguage fromLanguage(Language language) {
        if (language == null) {
            return UZ;
        }
        return switch (language) {
            case RU -> RU;
            case EN -> EN;
            case UZ -> UZ;
        };
    }

    public static SupportedLanguage fromLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            return UZ;
        }
        return switch (languageCode) {
            case RU -> RU;
            case EN -> EN;
            case UZ -> UZ;
        };
    }
}
