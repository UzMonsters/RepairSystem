package com.example.darks.repair_auto.shared.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TranslationCatalogCompletenessTest {

    private JsonTranslationCatalog catalog;
    private TranslationCatalogValidator validator;

    @BeforeEach
    void setUp() {
        catalog = new JsonTranslationCatalog(new ObjectMapper());
        catalog.init();
        validator = new TranslationCatalogValidator(catalog);
    }

    @Test
    void givenTranslationCatalogWhenValidatedThenNoMissingKeysOrPlaceholderMismatchesExist() {
        // This will throw IllegalStateException if UZ, RU, or EN keys or placeholders mismatch
        validator.validate();
    }

    @Test
    void givenAllErrorCodesWhenCheckedThenMessageKeyExistsInAllLanguages() {
        for (ErrorCode errorCode : ErrorCode.values()) {
            String key = errorCode.getMessageKey();
            assertThat(catalog.getMessage(key, SupportedLanguage.UZ))
                    .as("UZ message for error code %s (key=%s)", errorCode, key)
                    .isNotBlank();
            assertThat(catalog.getMessage(key, SupportedLanguage.RU))
                    .as("RU message for error code %s (key=%s)", errorCode, key)
                    .isNotBlank();
            assertThat(catalog.getMessage(key, SupportedLanguage.EN))
                    .as("EN message for error code %s (key=%s)", errorCode, key)
                    .isNotBlank();
        }
    }

    @Test
    void givenCatalogsWhenComparingFlattenedKeysThenKeysMatchExactlyAcrossLanguages() {
        Map<String, String> uz = catalog.getFlattenedCatalog(SupportedLanguage.UZ);
        Map<String, String> ru = catalog.getFlattenedCatalog(SupportedLanguage.RU);
        Map<String, String> en = catalog.getFlattenedCatalog(SupportedLanguage.EN);

        assertThat(uz.keySet())
                .as("UZ and RU catalogs must contain identical key sets")
                .containsExactlyInAnyOrderElementsOf(ru.keySet());

        assertThat(uz.keySet())
                .as("UZ and EN catalogs must contain identical key sets")
                .containsExactlyInAnyOrderElementsOf(en.keySet());
    }
}
