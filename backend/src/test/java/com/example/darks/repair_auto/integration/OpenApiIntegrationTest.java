package com.example.darks.repair_auto.integration;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void givenTestProfileWhenOpenApiDocsAreRequestedThenRepairAutoMetadataIsReturned()
            throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("RepairAuto API")))
                .andExpect(content().string(containsString("ApiErrorResponse")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.components.schemas.PageResponse.properties.first.type").value("boolean"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.components.schemas.PageResponse.properties.last.type").value("boolean"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.paths['/api/v1/users'].get.security[0].bearerAuth").isArray())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.components.schemas.LoginRequest.properties.password.format").value("password"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.components.schemas.LoginRequest.properties.password.writeOnly").value(true))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.components.schemas.UserCreateRequest.properties.password.writeOnly").value(true))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.components.schemas.PasswordChangeRequest.properties.currentPassword.writeOnly")
                        .value(true))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.components.schemas.PasswordChangeRequest.properties.newPassword.writeOnly")
                        .value(true))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.paths['/api/v1/users'].get.parameters[?(@.name == 'size')].description")
                        .value(org.hamcrest.Matchers.hasItem(containsString("1 to 100"))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.paths['/api/v1/users'].get.responses['401']").exists())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.paths['/api/v1/users'].get.responses['403']").exists())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.paths['/api/v1/customers'].get.security[0].bearerAuth").isArray())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.paths['/api/v1/technicians'].get.security[0].bearerAuth").isArray())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.paths['/api/v1/categories'].get.security[0].bearerAuth").isArray())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.paths['/api/v1/categories'].post.security[0].bearerAuth").isArray())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.paths['/api/v1/requests'].get.security[0].bearerAuth").isArray())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.paths['/api/v1/requests'].post.security[0].bearerAuth").isArray())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.paths['/api/v1/requests/{id}'].put.responses['409']").exists())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.paths['/api/v1/customers/{customerId}/requests'].get.security[0].bearerAuth")
                        .isArray())
                .andExpect(content().string(containsString("RepairRequestCreateRequest")))
                .andExpect(content().string(containsString("\"enum\":[\"EN\",\"RU\",\"UZ\"]")))
                .andExpect(content().string(containsString("nameEn")))
                .andExpect(content().string(containsString("descriptionEn")))
                .andExpect(content().string(containsString("REPAIR_REQUEST_NOT_EDITABLE")))
                .andExpect(content().string(containsString("telegramLinked")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("telegramUserId"))));
    }
}
