package com.hhplus.ecommerce.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class EmbeddedRedisConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedRedisConfig.class);
    private static final GenericContainer<?> REDIS_CONTAINER;

    static {
        REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
        REDIS_CONTAINER.start();
        log.info("Redis Testcontainer started on port {}", REDIS_CONTAINER.getFirstMappedPort());
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        // Set Redis properties from the container
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                applicationContext,
                "spring.data.redis.host=" + REDIS_CONTAINER.getHost(),
                "spring.data.redis.port=" + REDIS_CONTAINER.getFirstMappedPort()
        );

        log.info("Redis Testcontainer configured: {}:{}",
                REDIS_CONTAINER.getHost(),
                REDIS_CONTAINER.getFirstMappedPort());
    }
}
