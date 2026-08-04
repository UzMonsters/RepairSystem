package com.example.darks.repair_auto.shared.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class CorsConfigTest {

    @Test
    void givenWildcardOriginAndCredentialsWhenCorsSourceIsCreatedThenConfigurationFails() {
        AppProperties properties = new AppProperties(
                new AppProperties.Cors(List.of("*"), List.of("GET"), List.of("Content-Type"),
                        List.of("X-Trace-Id"), true),
                new AppProperties.Trace("X-Trace-Id"),
                new AppProperties.Jwt("test-secret-that-is-at-least-32-characters", "repair-auto", Duration.ofMinutes(15)),
                Duration.ofDays(30),
                new AppProperties.BootstrapAdmin(false, "", "", ""));

        assertThatThrownBy(() -> new CorsConfig().corsConfigurationSource(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Wildcard CORS origins");
    }

    @Test
    void givenExplicitOriginAndNoCredentialsWhenCorsSourceIsCreatedThenConfigurationIsValid() {
        AppProperties properties = new AppProperties(
                new AppProperties.Cors(List.of("http://localhost:3000"), List.of("GET"),
                        List.of("Content-Type"), List.of("X-Trace-Id"), false),
                new AppProperties.Trace("X-Trace-Id"),
                new AppProperties.Jwt("test-secret-that-is-at-least-32-characters", "repair-auto", Duration.ofMinutes(15)),
                Duration.ofDays(30),
                new AppProperties.BootstrapAdmin(false, "", "", ""));

        assertThatCode(() -> new CorsConfig().corsConfigurationSource(properties))
                .doesNotThrowAnyException();
    }
}
