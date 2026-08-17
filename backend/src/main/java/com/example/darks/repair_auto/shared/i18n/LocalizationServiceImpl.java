package com.example.darks.repair_auto.shared.i18n;

import jakarta.servlet.http.HttpServletRequest;
import java.text.MessageFormat;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LocalizationServiceImpl implements LocalizationService {

    private final JsonTranslationCatalog catalog;
    private final RequestLocaleResolver localeResolver;

    public LocalizationServiceImpl(JsonTranslationCatalog catalog, RequestLocaleResolver localeResolver) {
        this.catalog = catalog;
        this.localeResolver = localeResolver;
    }

    @Override
    public String get(String key) {
        return get(key, (Object[]) null);
    }

    @Override
    public String get(String key, Object... args) {
        SupportedLanguage language = localeResolver.resolveLanguage();
        return get(key, language, args);
    }

    @Override
    public String get(String key, HttpServletRequest request, Object... args) {
        SupportedLanguage language = localeResolver.resolveLanguage(request);
        return get(key, language, args);
    }

    @Override
    public String get(String key, SupportedLanguage language, Object... args) {
        if (key == null || key.isBlank()) {
            return "";
        }
        String normalizedKey = normalizeKey(key);
        SupportedLanguage targetLang = language != null ? language : localeResolver.resolveLanguage();
        String message = resolveWithFallback(normalizedKey, targetLang);

        if (message == null) {
            return key;
        }

        if (args != null && args.length > 0) {
            try {
                return MessageFormat.format(message, args);
            } catch (Exception e) {
                return message;
            }
        }

        return message;
    }

    private String normalizeKey(String key) {
        String trimmed = key.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return switch (trimmed.toLowerCase()) {
            case "must not be blank", "must not be null", "must not be empty",
                 "notblank", "notnull", "notempty",
                 "не должно быть пустым", "не должно быть null",
                 "bo'sh bo'lmasligi kerak", "null bo'lmasligi kerak",
                 "jakarta.validation.constraints.notblank.message",
                 "jakarta.validation.constraints.notnull.message",
                 "jakarta.validation.constraints.notempty.message" -> "validation.required";
            case "must be a well-formed email address", "email",
                 "jakarta.validation.constraints.email.message" -> "validation.email.invalid";
            default -> trimmed;
        };
    }

    private String resolveWithFallback(String key, SupportedLanguage primary) {
        List<SupportedLanguage> fallbackOrder = getFallbackOrder(primary);
        for (SupportedLanguage lang : fallbackOrder) {
            String msg = catalog.getMessage(key, lang);
            if (msg != null && !msg.isBlank()) {
                return msg;
            }
        }
        return null;
    }

    private List<SupportedLanguage> getFallbackOrder(SupportedLanguage primary) {
        if (primary == SupportedLanguage.RU) {
            return List.of(SupportedLanguage.RU, SupportedLanguage.UZ, SupportedLanguage.EN);
        } else if (primary == SupportedLanguage.EN) {
            return List.of(SupportedLanguage.EN, SupportedLanguage.UZ, SupportedLanguage.RU);
        } else {
            return List.of(SupportedLanguage.UZ, SupportedLanguage.RU, SupportedLanguage.EN);
        }
    }
}
