package com.hhplus.ecommerce.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testcontainers 기반 통합 테스트 설정
 * - MySQL 8.0 컨테이너를 사용하여 실제 DB 환경에서 테스트
 * - 모든 통합 테스트는 이 클래스를 상속받아 사용
 */
@SpringBootTest
@Testcontainers
public abstract class TestContainerConfig {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_ecommerce")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true); // 테스트 간 컨테이너 재사용으로 속도 향상

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }
}
