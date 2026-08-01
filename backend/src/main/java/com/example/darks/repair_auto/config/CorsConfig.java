package com.example.darks.repair_auto.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
        AppProperties.Cors cors = properties.cors();
        validate(cors);

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(emptyToNull(cors.allowedOrigins()));
        configuration.setAllowedMethods(cors.allowedMethods());
        configuration.setAllowedHeaders(cors.allowedHeaders());
        configuration.setExposedHeaders(cors.exposedHeaders());
        configuration.setAllowCredentials(cors.allowCredentials());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void validate(AppProperties.Cors cors) {
        if (cors.allowCredentials()
                && cors.allowedOrigins() != null
                && cors.allowedOrigins().contains("*")) {
            throw new IllegalStateException("Wildcard CORS origins cannot be used with credentials.");
        }
    }

    private List<String> emptyToNull(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values;
    }
}
