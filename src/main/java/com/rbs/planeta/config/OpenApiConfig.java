package com.rbs.planeta.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("API Portal do Patrocinador · Planeta Atlântida 2027")
                        .description("API RESTful desenvolvida em Spring Boot 3 para gestão executiva de cotas, mídias e arquivos de patrocinadores do Grupo RBS.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Grupo RBS · Núcleo B2B Eventos")
                                .email("comercial.planeta@gruporbs.com.br")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Insira o token JWT de autenticação do Supabase")
                        )
                );
    }
}
