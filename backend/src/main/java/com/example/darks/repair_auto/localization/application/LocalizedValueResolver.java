package com.example.darks.repair_auto.localization.application;

import com.example.darks.repair_auto.settings.domain.Language;
import org.springframework.stereotype.Component;

@Component
public class LocalizedValueResolver {

    /**
     * Resolves a localized string given the target language and values for UZ, RU, and EN.
     * Fallback order:
     * - Target RU: RU -> UZ -> EN
     * - Target EN: EN -> UZ -> RU
     * - Target UZ (or null): UZ -> RU -> EN
     *
     * Values count as unavailable when null, empty, or blank.
     */
    public String resolve(Language selectedLanguage, String uzValue, String ruValue, String enValue) {
        Language effectiveLanguage = selectedLanguage != null ? selectedLanguage : Language.UZ;

        String[] priorityList;
        switch (effectiveLanguage) {
            case RU -> priorityList = new String[]{ruValue, uzValue, enValue};
            case EN -> priorityList = new String[]{enValue, uzValue, ruValue};
            case UZ -> priorityList = new String[]{uzValue, ruValue, enValue};
            default -> priorityList = new String[]{uzValue, ruValue, enValue};
        }

        for (String candidate : priorityList) {
            if (isAvailable(candidate)) {
                return candidate.trim();
            }
        }

        return null;
    }

    private boolean isAvailable(String value) {
        return value != null && !value.isBlank();
    }
}
