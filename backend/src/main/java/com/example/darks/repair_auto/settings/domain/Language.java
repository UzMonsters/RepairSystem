package com.example.darks.repair_auto.settings.domain;

public enum Language {
    UZ,
    RU,
    EN;

    public static Language fromString(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.trim().toUpperCase();
        for (Language lang : values()) {
            if (lang.name().equals(normalized)) {
                return lang;
            }
        }
        // Also support standard BCP-47 / HTTP Accept-Language codes like "uz-UZ", "ru-RU", "en-US"
        if (normalized.startsWith("UZ")) return UZ;
        if (normalized.startsWith("RU")) return RU;
        if (normalized.startsWith("EN")) return EN;
        return null;
    }
}
