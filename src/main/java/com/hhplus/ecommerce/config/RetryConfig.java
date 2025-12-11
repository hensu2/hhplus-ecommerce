package com.hhplus.ecommerce.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
public class RetryConfig {
    // Spring Retry 활성화
    // @Retryable, @Recover 어노테이션을 사용할 수 있습니다.
}