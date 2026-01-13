package com.hhplus.ecommerce.scheduler;

import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import com.hhplus.ecommerce.infrastructure.order.jpa.OrderItemJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyPopularProductScheduler {

    private final OrderItemJpaRepository orderItemJpaRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String DAILY_POPULAR_KEY = "daily:popular:products";

    /**
     * 매일 자정 1분에 전날 판매 데이터를 집계하여 인기상품 캐시 갱신
     */
    @Scheduled(cron = "0 1 0 * * *") // 매일 00:01:00
    public void updateDailyPopularProducts() {
        log.info("Starting daily popular products update...");

        try {
            // 전날 00:00:00 ~ 23:59:59 타임스탬프 계산
            LocalDate yesterday = LocalDate.now().minusDays(1);
            long startOfDay = yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endOfDay = yesterday.atTime(23, 59, 59, 999_999_999)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            log.info("Aggregating sales from {} to {}",
                Instant.ofEpochMilli(startOfDay),
                Instant.ofEpochMilli(endOfDay));

            // 전날 주문 아이템 조회
            List<OrderItemEntity> yesterdayOrders = orderItemJpaRepository
                .findByCreatedAtBetween(startOfDay, endOfDay);

            if (yesterdayOrders.isEmpty()) {
                log.warn("No orders found for yesterday. Skipping update.");
                return;
            }

            // 상품별 판매량 집계
            Map<Long, Integer> productSalesMap = yesterdayOrders.stream()
                .collect(Collectors.groupingBy(
                    OrderItemEntity::getProductId,
                    Collectors.summingInt(OrderItemEntity::getQuantity)
                ));

            log.info("Aggregated {} products from {} orders",
                productSalesMap.size(),
                yesterdayOrders.size());

            // 기존 캐시 삭제
            redisTemplate.delete(DAILY_POPULAR_KEY);

            // Redis Sorted Set에 저장 (판매량을 score로 사용)
            productSalesMap.forEach((productId, salesCount) -> {
                redisTemplate.opsForZSet().add(
                    DAILY_POPULAR_KEY,
                    String.valueOf(productId),
                    salesCount.doubleValue()
                );
            });

            log.info("Successfully updated daily popular products cache with {} products",
                productSalesMap.size());

        } catch (Exception e) {
            log.error("Failed to update daily popular products", e);
        }
    }

    /**
     * 수동 실행용 메서드 (테스트/디버깅)
     */
    public void manualUpdate() {
        log.info("Manual update triggered");
        updateDailyPopularProducts();
    }
}
