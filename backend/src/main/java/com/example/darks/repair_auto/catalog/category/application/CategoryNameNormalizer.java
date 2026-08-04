package com.example.darks.repair_auto.catalog.category.application;

import java.text.Normalizer;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class CategoryNameNormalizer {

    public String normalize(String value) {
        if (value == null) {
            return "";
        }
        String compacted = value.trim().replaceAll("\\s+", " ");
        return Normalizer.normalize(compacted, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }
}
