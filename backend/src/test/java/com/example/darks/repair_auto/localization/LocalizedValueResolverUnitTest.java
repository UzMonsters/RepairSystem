package com.example.darks.repair_auto.localization;

import com.example.darks.repair_auto.localization.application.LocalizedValueResolver;
import com.example.darks.repair_auto.settings.domain.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalizedValueResolverUnitTest {

    private LocalizedValueResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new LocalizedValueResolver();
    }

    @Test
    void testEnglishSelected() {
        // EN exists -> EN
        assertThat(resolver.resolve(Language.EN, "Konditsioner", "Кондиционер", "Air Conditioner"))
                .isEqualTo("Air Conditioner");

        // EN missing -> UZ
        assertThat(resolver.resolve(Language.EN, "Konditsioner", "Кондиционер", null))
                .isEqualTo("Konditsioner");

        // EN + UZ missing -> RU
        assertThat(resolver.resolve(Language.EN, "", "Кондиционер", "   "))
                .isEqualTo("Кондиционер");
    }

    @Test
    void testRussianSelected() {
        // RU exists -> RU
        assertThat(resolver.resolve(Language.RU, "Konditsioner", "Кондиционер", "Air Conditioner"))
                .isEqualTo("Кондиционер");

        // RU missing -> UZ
        assertThat(resolver.resolve(Language.RU, "Konditsioner", null, "Air Conditioner"))
                .isEqualTo("Konditsioner");

        // RU + UZ missing -> EN
        assertThat(resolver.resolve(Language.RU, null, "   ", "Air Conditioner"))
                .isEqualTo("Air Conditioner");
    }

    @Test
    void testUzbekSelected() {
        // UZ exists -> UZ
        assertThat(resolver.resolve(Language.UZ, "Konditsioner", "Кондиционер", "Air Conditioner"))
                .isEqualTo("Konditsioner");

        // UZ missing -> RU
        assertThat(resolver.resolve(Language.UZ, null, "Кондиционер", "Air Conditioner"))
                .isEqualTo("Кондиционер");

        // UZ + RU missing -> EN
        assertThat(resolver.resolve(Language.UZ, "  ", null, "Air Conditioner"))
                .isEqualTo("Air Conditioner");
    }

    @Test
    void testNullOrBlankHandling() {
        assertThat(resolver.resolve(null, "Uzbek", "Russian", "English")).isEqualTo("Uzbek");
        assertThat(resolver.resolve(Language.EN, null, null, null)).isNull();
        assertThat(resolver.resolve(Language.EN, "  ", "", " \t ")).isNull();
    }
}
