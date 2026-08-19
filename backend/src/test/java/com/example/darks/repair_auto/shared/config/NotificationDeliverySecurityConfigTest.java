package com.example.darks.repair_auto.shared.config;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.application.EmailNormalizer;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.JwtAuthenticationFilter;
import com.example.darks.repair_auto.identity.infrastructure.security.JwtTokenService;
import com.example.darks.repair_auto.shared.error.ApiErrorResponseFactory;
import com.example.darks.repair_auto.shared.error.SecurityErrorHandler;
import com.example.darks.repair_auto.shared.i18n.LocalizationService;
import com.example.darks.repair_auto.shared.i18n.SupportedLanguage;
import com.example.darks.repair_auto.shared.observability.TraceIdService;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = NotificationDeliverySecurityConfigTest.NotificationDeliveryTestController.class)
@Import({SecurityConfig.class, NotificationDeliverySecurityConfigTest.TestBeans.class})
class NotificationDeliverySecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void givenManager_whenListNotificationDeliveries_thenAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/admin/notification-deliveries")
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenManager_whenRetryNotificationDelivery_thenForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/notification-deliveries/{deliveryId}/retry", 10L)
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAdmin_whenRetryNotificationDelivery_thenAllowed() throws Exception {
        mockMvc.perform(post("/api/v1/admin/notification-deliveries/{deliveryId}/retry", 10L)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @RestController
    public static class NotificationDeliveryTestController {

        @GetMapping("/api/v1/admin/notification-deliveries")
        public ResponseEntity<Void> list() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/api/v1/admin/notification-deliveries/{deliveryId}")
        public ResponseEntity<Void> detail(@PathVariable Long deliveryId) {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/api/v1/admin/notification-deliveries/{deliveryId}/retry")
        public ResponseEntity<Void> retry(@PathVariable Long deliveryId) {
            return ResponseEntity.ok().build();
        }
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(
                SecurityErrorHandler securityErrorHandler,
                UserRepository userRepository,
                CustomerRepository customerRepository,
                TechnicianRepository technicianRepository) {
            return new JwtAuthenticationFilter(
                    mock(JwtTokenService.class),
                    userRepository,
                    customerRepository,
                    technicianRepository,
                    securityErrorHandler);
        }

        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        CustomerRepository customerRepository() {
            return mock(CustomerRepository.class);
        }

        @Bean
        TechnicianRepository technicianRepository() {
            return mock(TechnicianRepository.class);
        }

        @Bean
        EmailNormalizer emailNormalizer() {
            return new EmailNormalizer();
        }

        @Bean
        TraceIdService traceIdService() {
            return new TraceIdService();
        }

        @Bean
        ApiErrorResponseFactory apiErrorResponseFactory(TraceIdService traceIdService) {
            return new ApiErrorResponseFactory(traceIdService);
        }

        @Bean
        AppProperties appProperties() {
            return new AppProperties(
                    new AppProperties.Cors(List.of(), List.of(), List.of(), List.of(), false),
                    new AppProperties.Trace("X-Trace-Id"),
                    new AppProperties.Jwt("test-local-only-jwt-secret-that-is-long-enough", "repair-auto", Duration.ofMinutes(15)),
                    Duration.ofDays(1),
                    Duration.ofDays(30),
                    new AppProperties.BootstrapAdmin(false, "", "", ""));
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        SecurityErrorHandler securityErrorHandler(
                AppProperties properties,
                ObjectMapper objectMapper,
                LocalizationService localizationService,
                ApiErrorResponseFactory apiErrorResponseFactory) {
            return new SecurityErrorHandler(
                    properties,
                    localizationService,
                    apiErrorResponseFactory,
                    objectMapper);
        }

        @Bean
        LocalizationService localizationService() {
            return new LocalizationService() {
                @Override
                public String get(String key) {
                    return key;
                }

                @Override
                public String get(String key, Object... args) {
                    return key;
                }

                @Override
                public String get(String key, SupportedLanguage language, Object... args) {
                    return key;
                }

                @Override
                public String get(String key, HttpServletRequest request, Object... args) {
                    return key;
                }
            };
        }
    }
}
