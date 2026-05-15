package com.imageflow.backend.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI imageFlowOpenApi(@Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        return new OpenAPI()
                .info(new Info()
                        .title("ImageFlow API")
                        .description("Image optimization, auth, upload, batch processing, and operations endpoints.")
                        .version("v1")
                        .contact(new Contact().name("ImageFlow")))
                .addServersItem(new Server().url(publicBaseUrl).description("Configured backend base URL"));
    }
}
