package com.example.darks.repair_auto.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class ProdProfileIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("repair_auto_prod_profile_test")
                    .withUsername("repair_auto")
                    .withPassword("repair_auto");

    static {
        POSTGRES.start();
    }

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret",
                () -> "prod-profile-test-jwt-secret-that-is-long-enough");
    }

    @Test
    void givenProdDefaultsWhenOpenApiIsRequestedThenItIsDisabled() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("\"code\":\"RESOURCE_NOT_FOUND\"")));
    }

    @Test
    void givenProdDefaultsWhenSwaggerUiIsRequestedThenItIsDisabled() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("\"code\":\"RESOURCE_NOT_FOUND\"")));
    }

    @Test
    void givenProdDefaultsWhenHealthIsRequestedThenDetailsDoNotLeakSensitiveData() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"status\":\"UP\"")))
                .andExpect(content().string(not(containsString("db"))))
                .andExpect(content().string(not(containsString("repair_auto"))))
                .andExpect(content().string(not(containsString("jdbc:postgresql"))));
    }

    @Test
    void givenProdDefaultsWhenInfoIsRequestedThenItIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    void givenProdDefaultsWhenSensitiveActuatorEndpointIsRequestedThenItIsNotPublic() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenProdDefaultsWhenCorsPreflightUsesLocalhostOriginThenItIsRejected() throws Exception {
        mockMvc.perform(options("/actuator/health")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
