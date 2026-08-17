package com.example.darks.repair_auto.shared.i18n;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JsonTranslationCatalog {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonTranslationCatalog.class);
    private final ObjectMapper objectMapper;
    private final Map<SupportedLanguage, Map<String, String>> catalogMap = new EnumMap<>(SupportedLanguage.class);

    public JsonTranslationCatalog(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        for (SupportedLanguage language : SupportedLanguage.values()) {
            String resourcePath = "/i18n/messages_" + language.getCode() + ".json";
            try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
                if (inputStream == null) {
                    throw new IllegalStateException("Translation file not found: " + resourcePath);
                }
                JsonNode root = objectMapper.readTree(inputStream);
                Map<String, String> flattened = new HashMap<>();
                flattenNode("", root, flattened);
                catalogMap.put(language, Map.copyOf(flattened));
                LOGGER.info("Loaded {} translation keys for language [{}]", flattened.size(), language.getCode());
            } catch (Exception e) {
                LOGGER.error("Failed to load translation catalog for language: {}", language, e);
                throw new IllegalStateException("Failed to initialize translation catalog for " + language, e);
            }
        }
    }

    private void flattenNode(String prefix, JsonNode node, Map<String, String> result) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                flattenNode(key, entry.getValue(), result);
            }
        } else if (node.isValueNode()) {
            result.put(prefix, node.asText());
        }
    }

    public String getMessage(String key, SupportedLanguage language) {
        if (key == null || language == null) {
            return null;
        }
        Map<String, String> map = catalogMap.get(language);
        return map != null ? map.get(key) : null;
    }

    public Map<String, String> getFlattenedCatalog(SupportedLanguage language) {
        Map<String, String> map = catalogMap.get(language);
        return map != null ? map : Collections.emptyMap();
    }
}
