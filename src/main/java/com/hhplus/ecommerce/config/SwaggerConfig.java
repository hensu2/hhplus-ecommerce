package com.hhplus.ecommerce.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 설정
 * - API 문서 자동 생성
 * - Swagger UI: http://localhost:8080/swagger-ui.html
 * - OpenAPI JSON: http://localhost:8080/v3/api-docs
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("항해플러스 E-Commerce API")
                        .description("""
                                항해플러스 E-Commerce 백엔드 API 명세서

                                ## 주요 기능
                                - 사용자 관리 및 포인트 시스템
                                - 상품 조회 및 주문 처리
                                - 결제 처리 및 재고 관리
                                - 인기 상품 조회

                                ## 동시성 제어
                                - 포인트 충전/사용: JPA @Version 낙관적 락
                                - 재고 감소: JPA @Version 낙관적 락

                                ## 아키텍처
                                - Clean Architecture (Presentation → Application → Domain → Infrastructure)
                                - JPA/Hibernate with MySQL
                                - Spring Boot 3.x
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("정현수")
                                .email("hensu2@naver.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.hhplus-ecommerce.com")
                                .description("Production Server (예시)")
                ));
    }
}