package com.example.darks.repair_auto.shared.i18n;

import jakarta.servlet.http.HttpServletRequest;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class LocalizationServiceImpl implements LocalizationService {

    private final JsonTranslationCatalog catalog;
    private final RequestLocaleResolver requestLocaleResolver;

    public LocalizationServiceImpl(
            JsonTranslationCatalog catalog,
            RequestLocaleResolver requestLocaleResolver) {
        this.catalog = catalog;
        this.requestLocaleResolver = requestLocaleResolver;
    }

    @Override
    public String get(String key) {
        return get(key, requestLocaleResolver.resolveLanguage());
    }

    @Override
    public String get(String key, Object... args) {
        return get(key, requestLocaleResolver.resolveLanguage(), args);
    }

    @Override
    public String get(String key, SupportedLanguage language, Object... args) {
        SupportedLanguage targetLanguage = language != null ? language : SupportedLanguage.UZ;
        String normalizedKey = normalizeKey(key);
        String template = resolveTemplate(normalizedKey, targetLanguage);
        return format(template, args);
    }

    @Override
    public String get(String key, Locale locale, Object... args) {
        SupportedLanguage language = locale != null ? SupportedLanguage.fromCode(locale.getLanguage()) : null;
        return get(key, language, args);
    }

    @Override
    public String get(String key, HttpServletRequest request, Object... args) {
        SupportedLanguage language = request != null
                ? requestLocaleResolver.resolveLanguage(request)
                : requestLocaleResolver.resolveLanguage();
        return get(key, language, args);
    }

    private String resolveTemplate(String normalizedKey, SupportedLanguage language) {
        SupportedLanguage primary = language != null ? language : SupportedLanguage.UZ;
        String message = catalog.getMessage(normalizedKey, primary);
        if (message != null) {
            return message;
        }
        for (SupportedLanguage fallback : getFallbackOrder(primary)) {
            message = catalog.getMessage(normalizedKey, fallback);
            if (message != null) {
                return message;
            }
        }
        return normalizedKey;
    }

    private List<SupportedLanguage> getFallbackOrder(SupportedLanguage primary) {
        return switch (primary) {
            case UZ -> List.of(SupportedLanguage.RU, SupportedLanguage.EN);
            case RU -> List.of(SupportedLanguage.UZ, SupportedLanguage.EN);
            case EN -> List.of(SupportedLanguage.UZ, SupportedLanguage.RU);
        };
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            return "common.internal-error";
        }
        String cleaned = key.strip();
        if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).strip();
        }
        if (cleaned.startsWith("jakarta.validation.constraints.")
                || cleaned.startsWith("org.hibernate.validator.constraints.")) {
            String shortName = cleaned.substring(cleaned.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            if (shortName.endsWith(".message")) {
                shortName = shortName.substring(0, shortName.length() - ".message".length());
            }
            return "validation." + shortName;
        }
        if (cleaned.startsWith("validation.")
                || cleaned.startsWith("common.")
                || cleaned.startsWith("security.")
                || cleaned.startsWith("auth.")
                || cleaned.startsWith("user.")
                || cleaned.startsWith("customer.")
                || cleaned.startsWith("technician.")
                || cleaned.startsWith("category.")
                || cleaned.startsWith("repair.")
                || cleaned.startsWith("attachment.")
                || cleaned.startsWith("telegram.")
                || cleaned.startsWith("chat.")
                || cleaned.startsWith("request.")
                || cleaned.startsWith("dashboard.")
                || cleaned.startsWith("notification.")
                || cleaned.startsWith("review.")) {
            return cleaned;
        }
        return switch (cleaned) {
            case "must not be blank", "ne bo'sh bo'lmasligi kerak", "не должно быть пустым", "NotBlank" ->
                    "validation.not-blank";
            case "must not be null", "NotNull" -> "validation.not-null";
            case "must be greater than 0", "Positive" -> "validation.positive";
            case "must be greater than or equal to 0", "PositiveOrZero" -> "validation.positive-or-zero";
            case "must be a well-formed email address", "Email" -> "validation.email";
            case "must be a well-formed phone number", "Pattern" -> "validation.pattern";
            case "Size" -> "validation.size";
            case "Min" -> "validation.min";
            case "Max" -> "validation.max";
            case "DecimalMin" -> "validation.decimal-min";
            case "DecimalMax" -> "validation.decimal-max";
            default -> cleaned;
        };
    }

    private String format(String template, Object... args) {
        if (template == null || args == null || args.length == 0) {
            return template;
        }
        try {
            // In Java MessageFormat, single quotes are syntax characters.
            // If the string contains single quotes that aren't already doubled,
            // we escape them so words like "o'tishi", "Ta'mirlash" don't break placeholder substitution.
            String escapedTemplate = template.contains("'")
                    ? template.replace("''", "'").replace("'", "''")
                    : template;
            return MessageFormat.format(escapedTemplate, args);
        } catch (IllegalArgumentException ex) {
            return template;
        }
    }
}
