package com.xenotask.xeno.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Xeno API")
                        .description("Interactive documentation for the Xeno multi-tenant backend")
                        .version("v1")
                        .license(new License().name("Proprietary"))
                        .contact(new Contact().name("Xeno Team")))
                .externalDocs(new ExternalDocumentation()
                        .description("Project Documentation")
                        .url("https://github.com/kumarDivyanshu/XenoTask/blob/main/documentation.md"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                );
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("auth")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi analyticsApi() {
        return GroupedOpenApi.builder()
                .group("analytics")
                .pathsToMatch("/api/analytics/**")
                .build();
    }

    @Bean
    public GroupedOpenApi syncApi() {
        return GroupedOpenApi.builder()
                .group("sync")
                .pathsToMatch("/api/sync/**", "/api/sync/jobs/**")
                .build();
    }

    @Bean
    public GroupedOpenApi tenantsApi() {
        return GroupedOpenApi.builder()
                .group("tenants")
                .pathsToMatch("/api/tenants/**", "/api/tenant-access/**")
                .build();
    }

    @Bean
    public GroupedOpenApi eventsApi() {
        return GroupedOpenApi.builder()
                .group("events")
                .pathsToMatch("/api/events/**")
                .build();
    }
}

