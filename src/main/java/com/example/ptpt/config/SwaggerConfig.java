package com.example.ptpt.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;


@Configuration
public class SwaggerConfig {

    @Value("${springdoc.server-url}")
    private String serverUrl;

    @Bean
    public OpenAPI customOpenAPI() {

//        https://kwakscoding.tistory.com/49
        Server server = new Server();
        server.setUrl(serverUrl);

        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 인증을 위한 토큰을 입력해주세요."))
                )
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .addServersItem(server)
                .info(info());
    }

    private Info info() {
        return new Info()
                .title("PTPT API")
                .description("PTPT 서비스 API 명세서")
                .version("1.0.0");
    }
}
