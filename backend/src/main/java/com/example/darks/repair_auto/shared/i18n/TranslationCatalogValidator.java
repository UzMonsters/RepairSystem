package com.example.darks.repair_auto.shared.i18n;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TranslationCatalogValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TranslationCatalogValidator.class);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(\\d+)\\}");

    private final JsonTranslationCatalog catalog;

    public TranslationCatalogValidator(JsonTranslationCatalog catalog) {
        this.catalog = catalog;
    }

    @PostConstruct
    public void validate() {
        Map<String, String> uzCatalog = catalog.getFlattenedCatalog(SupportedLanguage.UZ);
        Map<String, String> ruCatalog = catalog.getFlattenedCatalog(SupportedLanguage.RU);
        Map<String, String> enCatalog = catalog.getFlattenedCatalog(SupportedLanguage.EN);

        List<String> errors = new ArrayList<>();

        // Validate key equality across UZ, RU, EN
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(uzCatalog.keySet());
        allKeys.addAll(ruCatalog.keySet());
        allKeys.addAll(enCatalog.keySet());

        for (String key : allKeys) {
            if (!uzCatalog.containsKey(key)) {
                errors.add("Missing translation: locale=uz, key=" + key);
            }
            if (!ruCatalog.containsKey(key)) {
                errors.add("Missing translation: locale=ru, key=" + key);
            }
            if (!enCatalog.containsKey(key)) {
                errors.add("Missing translation: locale=en, key=" + key);
            }

            // Check non-blank
            checkNonBlank("uz", key, uzCatalog.get(key), errors);
            checkNonBlank("ru", key, ruCatalog.get(key), errors);
            checkNonBlank("en", key, enCatalog.get(key), errors);

            // Check placeholder parity
            if (uzCatalog.containsKey(key) && ruCatalog.containsKey(key) && enCatalog.containsKey(key)) {
                Set<String> uzPlaceholders = extractPlaceholders(uzCatalog.get(key));
                Set<String> ruPlaceholders = extractPlaceholders(ruCatalog.get(key));
                Set<String> enPlaceholders = extractPlaceholders(enCatalog.get(key));

                if (!uzPlaceholders.equals(ruPlaceholders) || !uzPlaceholders.equals(enPlaceholders)) {
                    errors.add("Placeholder mismatch for key=" + key
                            + " [uz=" + uzPlaceholders + ", ru=" + ruPlaceholders + ", en=" + enPlaceholders + "]");
                }
            }
        }

        if (!errors.isEmpty()) {
            String combinedError = "Translation catalog validation failed:\n" + String.join("\n", errors);
            LOGGER.error(combinedError);
            throw new IllegalStateException(combinedError);
        }

        LOGGER.info("Translation catalog validation succeeded. All {} keys match across UZ, RU, EN.", uzCatalog.size());
    }

    private void checkNonBlank(String locale, String key, String value, List<String> errors) {
        if (value != null && value.isBlank()) {
            errors.add("Blank translation: locale=" + locale + ", key=" + key);
        }
    }

    private Set<String> extractPlaceholders(String text) {
        Set<String> placeholders = new HashSet<>();
        if (text == null) {
            return placeholders;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }
        return placeholders;
    }
}
