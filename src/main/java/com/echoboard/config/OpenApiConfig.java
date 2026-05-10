package com.echoboard.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI echoBoardOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(apiServers())
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(
                        SECURITY_SCHEME_NAME,
                        bearerAuthSecurityScheme()
                ));
    }

    private Info apiInfo() {
        return new Info()
                .title("EchoBoard Backend API")
                .version("1.0.0")
                .description("""
                        EchoBoard is a real-time Q&A, polling, analytics, and audience interaction backend.

                        Core features include JWT authentication, participant-scoped tokens, WebSocket/STOMP broadcasts,
                        Redis-backed rate limiting and presence tracking, RabbitMQ async analytics workflows,
                        scheduled jobs, CSV export, local file uploads, Docker Compose, and CI.
                        """)
                .contact(new Contact()
                        .name("Mohamed Abdul Shafi")
                        .email("mohamedsadik763@gmail.com")
                        .url("https://github.com/Abdulshafi2612"))
                .license(new License()
                        .name("Github Repo")
                        .url("https://github.com/Abdulshafi2612/EchoBoard"));
    }

    private List<Server> apiServers() {
        return List.of(
                new Server()
                        .url("http://localhost:8080")
                        .description("Local development server")
        );
    }

    private SecurityScheme bearerAuthSecurityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter a valid JWT token. Example: Bearer eyJhbGciOi...");
    }
}