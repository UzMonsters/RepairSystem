package com.example.darks.repair_auto.localization;

import com.example.darks.repair_auto.localization.application.LocalizedValueResolver;
import com.example.darks.repair_auto.settings.domain.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalizedValueResolverTest {

    private LocalizedValueResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new LocalizedValueResolver();
    }

    @Test
    @DisplayName("selected UZ + all translations present -> UZ")
    void selectedUzAllPresent() {
        String result = resolver.resolve(Language.UZ, "Uzbek", "Russian", "English");
        assertThat(result).isEqualTo("Uzbek");
    }

    @Test
    @DisplayName("selected RU + all translations present -> RU")
    void selectedRuAllPresent() {
        String result = resolver.resolve(Language.RU, "Uzbek", "Russian", "English");
        assertThat(result).isEqualTo("Russian");
    }

    @Test
    @DisplayName("selected EN + all translations present -> EN")
    void selectedEnAllPresent() {
        String result = resolver.resolve(Language.EN, "Uzbek", "Russian", "English");
        assertThat(result).isEqualTo("English");
    }

    @Test
    @DisplayName("selected RU + RU missing -> UZ")
    void selectedRuMissingRu() {
        String result = resolver.resolve(Language.RU, "Uzbek", "", "English");
        assertThat(result).isEqualTo("Uzbek");
    }

    @Test
    @DisplayName("selected EN + EN missing -> UZ")
    void selectedEnMissingEn() {
        String result = resolver.resolve(Language.EN, "Uzbek", "Russian", null);
        assertThat(result).isEqualTo("Uzbek");
    }

    @Test
    @DisplayName("selected EN + EN/UZ missing -> RU")
    void selectedEnMissingEnUz() {
        String result = resolver.resolve(Language.EN, "  ", "Russian", " ");
        assertThat(result).isEqualTo("Russian");
    }

    @Test
    @DisplayName("selected RU + RU/UZ missing -> EN")
    void selectedRuMissingRuUz() {
        String result = resolver.resolve(Language.RU, null, "", "English");
        assertThat(result).isEqualTo("English");
    }

    @Test
    @DisplayName("selected UZ + UZ missing -> RU")
    void selectedUzMissingUz() {
        String result = resolver.resolve(Language.UZ, "", "Russian", "English");
        assertThat(result).isEqualTo("Russian");
    }

    @Test
    @DisplayName("selected UZ + UZ/RU missing -> EN")
    void selectedUzMissingUzRu() {
        String result = resolver.resolve(Language.UZ, null, " ", "English");
        assertThat(result).isEqualTo("English");
    }

    @Test
    @DisplayName("null selected language -> defaults to UZ priority")
    void nullSelectedLanguage() {
        String result = resolver.resolve(null, "Uzbek", "Russian", "English");
        assertThat(result).isEqualTo("Uzbek");
    }

    @Test
    @DisplayName("blank string treated as unavailable")
    void blankStringTreatedAsUnavailable() {
        String result = resolver.resolve(Language.RU, "Uzbek", "   \t\n  ", "English");
        assertThat(result).isEqualTo("Uzbek");
    }

    @Test
    @DisplayName("all unavailable returns null")
    void allUnavailableReturnsNull() {
        String result = resolver.resolve(Language.UZ, " ", null, "");
        assertThat(result).isNull();
    }
}
