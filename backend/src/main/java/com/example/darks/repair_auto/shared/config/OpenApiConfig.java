package com.example.darks.repair_auto.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI repairAutoOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("RepairAuto API")
                        .version("0.0.1")
                        .description("""
                                Backend foundation for the RepairAuto repair-service CRM.
                                Application APIs are versioned under /api/v1.
                                Pagination uses page=0, size=20, sort=createdAt,desc.
                                API timestamps use ISO 8601 and database timestamps are stored in UTC.
                                """))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    @Bean
    OpenApiCustomizer foundationSchemaCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents()
                    .addSchemas("ApiErrorResponse", errorSchema())
                    .addSchemas("FieldErrorResponse", fieldErrorSchema())
                    .addSchemas("PageResponse", pageSchema());
        };
    }

    private ObjectSchema errorSchema() {
        ObjectSchema schema = new ObjectSchema();
        schema.addProperty("timestamp", new StringSchema().format("date-time"));
        schema.addProperty("status", new IntegerSchema().format("int32"));
        schema.addProperty("code", new StringSchema());
        schema.addProperty("message", new StringSchema());
        schema.addProperty("path", new StringSchema());
        schema.addProperty("traceId", new StringSchema());
        schema.addProperty("fieldErrors", new ArraySchema().items(fieldErrorSchema()));
        return schema;
    }

    private ObjectSchema fieldErrorSchema() {
        ObjectSchema schema = new ObjectSchema();
        schema.addProperty("field", new StringSchema());
        schema.addProperty("code", new StringSchema());
        schema.addProperty("message", new StringSchema());
        return schema;
    }

    private ObjectSchema pageSchema() {
        ObjectSchema schema = new ObjectSchema();
        schema.addProperty("content", new ArraySchema());
        schema.addProperty("page", new IntegerSchema().format("int32"));
        schema.addProperty("size", new IntegerSchema().format("int32"));
        schema.addProperty("totalElements", new IntegerSchema().format("int64"));
        schema.addProperty("totalPages", new IntegerSchema().format("int32"));
        schema.addProperty("first", new BooleanSchema());
        schema.addProperty("last", new BooleanSchema());
        return schema;
    }
}
