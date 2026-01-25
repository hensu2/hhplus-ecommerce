package com.hhplus.ecommerce.infrastructure.product;

import com.hhplus.ecommerce.domain.product.ProductStatisticsEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ProductStatisticsRepositoryImpl implements ProductStatisticsRepository {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String STATS_KEY_PREFIX = "product:stats:";
    private static final String DAILY_POPULAR_KEY = "daily:popular:products";

    private static final String FIELD_VIEW_COUNT = "viewCount";
    private static final String FIELD_SALES_COUNT = "salesCount";
    private static final String FIELD_UPDATED_AT = "updatedAt";

    @Override
    public ProductStatisticsEntity save(ProductStatisticsEntity statistics) {
        String key = STATS_KEY_PREFIX + statistics.getProductId();

        // Hash에 통계 데이터 저장 (조회수만 실시간 업데이트)
        Map<String, String> fields = new HashMap<>();
        fields.put(FIELD_VIEW_COUNT, String.valueOf(statistics.getViewCount()));
        fields.put(FIELD_SALES_COUNT, String.valueOf(statistics.getSalesCount()));
        fields.put(FIELD_UPDATED_AT, String.valueOf(statistics.getUpdatedAt()));

        redisTemplate.opsForHash().putAll(key, fields);

        return statistics;
    }

    @Override
    public Optional<ProductStatisticsEntity> findByProductId(long productId) {
        String key = STATS_KEY_PREFIX + productId;

        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        if (entries.isEmpty()) {
            return Optional.empty();
        }

        Long viewCount = Long.parseLong((String) entries.get(FIELD_VIEW_COUNT));
        Long salesCount = Long.parseLong((String) entries.get(FIELD_SALES_COUNT));
        Long updatedAt = Long.parseLong((String) entries.get(FIELD_UPDATED_AT));

        return Optional.of(new ProductStatisticsEntity(productId, viewCount, salesCount, updatedAt));
    }

    @Override
    public List<ProductStatisticsEntity> findAll() {
        // 일간 인기상품 Sorted Set에서 조회 (전날 판매량 기준)
        Set<ZSetOperations.TypedTuple<String>> productScores =
            redisTemplate.opsForZSet().reverseRangeWithScores(DAILY_POPULAR_KEY, 0, -1);

        if (productScores == null || productScores.isEmpty()) {
            return Collections.emptyList();
        }

        return productScores.stream()
            .map(tuple -> {
                long productId = Long.parseLong(tuple.getValue());
                long salesCount = tuple.getScore().longValue();

                // 전날 판매량 기준으로 ProductStatisticsEntity 생성
                return new ProductStatisticsEntity(
                    productId,
                    0L, // 조회수는 인기상품 순위에 미반영
                    salesCount,
                    System.currentTimeMillis()
                );
            })
            .collect(Collectors.toList());
    }
}
