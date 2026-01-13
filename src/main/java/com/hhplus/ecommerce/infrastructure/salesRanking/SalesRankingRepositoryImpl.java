package com.hhplus.ecommerce.infrastructure.salesRanking;

import com.hhplus.ecommerce.domain.salesRanking.SalesRankingItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class SalesRankingRepositoryImpl implements SalesRankingRepository {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void incrementScore(String key, Long productId, Integer quantity) {
        redisTemplate.opsForZSet().incrementScore(key, productId.toString(), quantity);
    }

    @Override
    public void decrementScore(String key, Long productId, Integer quantity) {
        redisTemplate.opsForZSet().incrementScore(key, productId.toString(), -quantity);
    }

    @Override
    public void setExpire(String key, long timeout, TimeUnit timeUnit) {
        redisTemplate.expire(key, timeout, timeUnit);
    }

    @Override
    public List<SalesRankingItem> getRankingWithScores(String key, long start, long end) {
        Set<ZSetOperations.TypedTuple<String>> results =
            redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);

        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }

        long rank = start + 1;
        List<SalesRankingItem> items = new ArrayList<>();

        for (ZSetOperations.TypedTuple<String> tuple : results) {
            items.add(new SalesRankingItem(
                rank++,
                Long.parseLong(tuple.getValue()),
                tuple.getScore().longValue()
            ));
        }

        return items;
    }

    @Override
    public long getSize(String key) {
        Long size = redisTemplate.opsForZSet().zCard(key);
        return size != null ? size : 0L;
    }
}