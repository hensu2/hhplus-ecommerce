package com.hhplus.ecommerce.infrastructure.salesRanking;

import com.hhplus.ecommerce.domain.salesRanking.SalesRankingItem;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface SalesRankingRepository {
    void incrementScore(String key, Long productId, Integer quantity);
    void decrementScore(String key, Long productId, Integer quantity);
    void setExpire(String key, long timeout, TimeUnit timeUnit);
    List<SalesRankingItem> getRankingWithScores(String key, long start, long end);
    long getSize(String key);
}