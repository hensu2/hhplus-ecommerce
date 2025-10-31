package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("hhplus-ecommerce API")
                        .description("Spring Boot ecommerce REST API Documentation")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("hensu2")
                                .email("hensu2@naver.com")));
    }
}