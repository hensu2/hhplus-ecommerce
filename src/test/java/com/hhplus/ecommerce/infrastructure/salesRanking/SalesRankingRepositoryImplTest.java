package com.hhplus.ecommerce.infrastructure.salesRanking;

import com.hhplus.ecommerce.config.EmbeddedRedisConfig;
import com.hhplus.ecommerce.domain.salesRanking.SalesRankingItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(initializers = EmbeddedRedisConfig.class)
class SalesRankingRepositoryImplTest {

    @Autowired
    private SalesRankingRepository salesRankingRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String TEST_KEY = "test:ranking:key";

    @AfterEach
    void cleanup() {
        redisTemplate.delete(TEST_KEY);
    }

    @Test
    @DisplayName("판매 수량 증가")
    void incrementScore() {
        // given
        Long productId = 1L;
        Integer quantity = 10;

        // when
        salesRankingRepository.incrementScore(TEST_KEY, productId, quantity);
        salesRankingRepository.incrementScore(TEST_KEY, productId, 5);

        // then
        Double score = redisTemplate.opsForZSet().score(TEST_KEY, productId.toString());
        assertThat(score).isEqualTo(15.0);
    }

    @Test
    @DisplayName("판매 수량 감소")
    void decrementScore() {
        // given
        Long productId = 1L;
        salesRankingRepository.incrementScore(TEST_KEY, productId, 20);

        // when
        salesRankingRepository.decrementScore(TEST_KEY, productId, 7);

        // then
        Double score = redisTemplate.opsForZSet().score(TEST_KEY, productId.toString());
        assertThat(score).isEqualTo(13.0);
    }

    @Test
    @DisplayName("TTL 설정")
    void setExpire() {
        // given
        Long productId = 1L;
        salesRankingRepository.incrementScore(TEST_KEY, productId, 10);

        // when
        salesRankingRepository.setExpire(TEST_KEY, 10, TimeUnit.SECONDS);

        // then
        Long ttl = redisTemplate.getExpire(TEST_KEY, TimeUnit.SECONDS);
        assertThat(ttl).isGreaterThan(0L);
        assertThat(ttl).isLessThanOrEqualTo(10L);
    }

    @Test
    @DisplayName("랭킹 조회 - 점수 높은 순으로 정렬")
    void getRankingWithScores() {
        // given
        salesRankingRepository.incrementScore(TEST_KEY, 1L, 50);
        salesRankingRepository.incrementScore(TEST_KEY, 2L, 30);
        salesRankingRepository.incrementScore(TEST_KEY, 3L, 100);
        salesRankingRepository.incrementScore(TEST_KEY, 4L, 20);

        // when
        List<SalesRankingItem> rankings = salesRankingRepository.getRankingWithScores(TEST_KEY, 0, 2);

        // then
        assertThat(rankings).hasSize(3);
        assertThat(rankings.get(0).getProductId()).isEqualTo(3L);
        assertThat(rankings.get(0).getSalesCount()).isEqualTo(100L);
        assertThat(rankings.get(0).getRank()).isEqualTo(1L);
        assertThat(rankings.get(1).getProductId()).isEqualTo(1L);
        assertThat(rankings.get(1).getSalesCount()).isEqualTo(50L);
        assertThat(rankings.get(1).getRank()).isEqualTo(2L);
        assertThat(rankings.get(2).getProductId()).isEqualTo(2L);
        assertThat(rankings.get(2).getSalesCount()).isEqualTo(30L);
        assertThat(rankings.get(2).getRank()).isEqualTo(3L);
    }

    @Test
    @DisplayName("랭킹 크기 조회")
    void getSize() {
        // given
        salesRankingRepository.incrementScore(TEST_KEY, 1L, 10);
        salesRankingRepository.incrementScore(TEST_KEY, 2L, 20);
        salesRankingRepository.incrementScore(TEST_KEY, 3L, 30);

        // when
        long size = salesRankingRepository.getSize(TEST_KEY);

        // then
        assertThat(size).isEqualTo(3L);
    }

    @Test
    @DisplayName("빈 키 조회 시 빈 리스트 반환")
    void getRankingWithScores_emptyKey() {
        // when
        List<SalesRankingItem> rankings = salesRankingRepository.getRankingWithScores("non:existent:key", 0, 10);

        // then
        assertThat(rankings).isEmpty();
    }

    @Test
    @DisplayName("빈 키 크기 조회 시 0 반환")
    void getSize_emptyKey() {
        // when
        long size = salesRankingRepository.getSize("non:existent:key");

        // then
        assertThat(size).isEqualTo(0L);
    }
}