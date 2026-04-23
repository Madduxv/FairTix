package com.fairtix.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fairtixOpenAPI() {
        String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("FairTix API")
                        .version("1.0.0")
                        .description("Fair ticketing platform API for event management, "
                                + "seat inventory, holds, and administration."))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components()
                        .addSecuritySchemes(schemeName, new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste a JWT obtained from POST /auth/login")));
    }
}
