package com.hhplus.ecommerce.integration;

import com.hhplus.ecommerce.application.coupon.IssueCouponUseCase;
import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.DiscountType;
import com.hhplus.ecommerce.infrastructure.coupon.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Redis 기반 쿠폰 발급 동시성 테스트")
class RedisCouponConcurrencyTest {

    @Autowired
    private IssueCouponUseCase issueCouponUseCase;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        // Redis 초기화
        redisTemplate.keys("coupon:*").forEach(key -> redisTemplate.delete(key));
    }

    @Test
    @DisplayName("100명이 동시에 100개 쿠폰 발급 - 정확히 100명만 성공")
    void concurrentIssue100Users100Stock() throws InterruptedException {
        // given
        int stock = 100;
        int threadCount = 100;
        CouponEntity coupon = createTestCoupon(stock);
        Long couponId = coupon.getId();

        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 100명이 동시에 발급 시도
        for (long userId = 1; userId <= threadCount; userId++) {
            long finalUserId = userId;
            executorService.submit(() -> {
                try {
                    issueCouponUseCase.execute(finalUserId, couponId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        System.out.println("========== Redis 동시성 테스트 결과 (100/100) ==========");
        System.out.println("초기 재고: " + stock);
        System.out.println("요청 수: " + threadCount);
        System.out.println("성공 수: " + successCount.get());
        System.out.println("실패 수: " + failCount.get());

        String remainingStock = redisTemplate.opsForValue().get("coupon:stock:" + couponId);
        System.out.println("남은 재고 (Redis): " + remainingStock);
        System.out.println("====================================================");

        // 정확히 100명만 성공해야 함
        assertThat(successCount.get()).isEqualTo(stock);
        assertThat(failCount.get()).isEqualTo(0);
        assertThat(Integer.parseInt(remainingStock)).isEqualTo(0);

        // 큐 사이즈 확인
        Long queueSize = redisTemplate.opsForList().size("coupon:issue:queue:" + couponId);
        assertThat(queueSize).isEqualTo(stock);

        // 중복 방지 Set 확인
        Long issuedCount = redisTemplate.opsForSet().size("coupon:issued:" + couponId);
        assertThat(issuedCount).isEqualTo(stock);
    }

    @Test
    @DisplayName("1000명이 100개 쿠폰에 동시 발급 - 100명만 성공, 900명 실패")
    void concurrentIssue1000Users100Stock() throws InterruptedException {
        // given
        int stock = 100;
        int threadCount = 1000;
        CouponEntity coupon = createTestCoupon(stock);
        Long couponId = coupon.getId();

        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 1000명이 동시에 발급 시도
        for (long userId = 1; userId <= threadCount; userId++) {
            long finalUserId = userId;
            executorService.submit(() -> {
                try {
                    issueCouponUseCase.execute(finalUserId, couponId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        System.out.println("========== Redis 동시성 테스트 결과 (1000/100) ==========");
        System.out.println("초기 재고: " + stock);
        System.out.println("요청 수: " + threadCount);
        System.out.println("성공 수: " + successCount.get());
        System.out.println("실패 수: " + failCount.get());

        String remainingStock = redisTemplate.opsForValue().get("coupon:stock:" + couponId);
        System.out.println("남은 재고 (Redis): " + remainingStock);
        System.out.println("======================================================");

        // 정확히 100명만 성공해야 함
        assertThat(successCount.get()).isEqualTo(stock);
        assertThat(failCount.get()).isEqualTo(threadCount - stock);
        assertThat(Integer.parseInt(remainingStock)).isEqualTo(0);

        // 큐 사이즈 확인
        Long queueSize = redisTemplate.opsForList().size("coupon:issue:queue:" + couponId);
        assertThat(queueSize).isEqualTo(stock);

        // 중복 방지 Set 확인
        Long issuedCount = redisTemplate.opsForSet().size("coupon:issued:" + couponId);
        assertThat(issuedCount).isEqualTo(stock);
    }

    @Test
    @DisplayName("동일 사용자가 10번 동시 발급 시도 - 1번만 성공")
    void sameUserMultipleTimes() throws InterruptedException {
        // given
        long userId = 999L;
        int stock = 10;
        int threadCount = 10;
        CouponEntity coupon = createTestCoupon(stock);
        Long couponId = coupon.getId();

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 같은 사용자가 10번 동시 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    issueCouponUseCase.execute(userId, couponId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        System.out.println("========== 중복 발급 방지 테스트 결과 ==========");
        System.out.println("발급 시도 횟수: " + threadCount);
        System.out.println("성공 수: " + successCount.get());
        System.out.println("실패 수: " + failCount.get());
        System.out.println("===========================================");

        // 1번만 성공해야 함
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(9);

        // 중복 방지 Set 확인
        Boolean isMember = redisTemplate.opsForSet()
            .isMember("coupon:issued:" + couponId, String.valueOf(userId));
        assertThat(isMember).isTrue();

        // 재고는 1개만 차감되어야 함
        String remainingStock = redisTemplate.opsForValue().get("coupon:stock:" + couponId);
        assertThat(Integer.parseInt(remainingStock)).isEqualTo(9);
    }

    @Test
    @DisplayName("대규모 동시 요청 - 5000명이 500개 쿠폰 발급")
    void largeScaleConcurrency() throws InterruptedException {
        // given
        int stock = 500;
        int threadCount = 5000;
        CouponEntity coupon = createTestCoupon(stock);
        Long couponId = coupon.getId();

        ExecutorService executorService = Executors.newFixedThreadPool(200);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // when - 5000명이 동시에 발급 시도
        for (long userId = 1; userId <= threadCount; userId++) {
            long finalUserId = userId;
            executorService.submit(() -> {
                try {
                    issueCouponUseCase.execute(finalUserId, couponId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // then
        System.out.println("========== 대규모 동시성 테스트 결과 (5000/500) ==========");
        System.out.println("초기 재고: " + stock);
        System.out.println("요청 수: " + threadCount);
        System.out.println("성공 수: " + successCount.get());
        System.out.println("실패 수: " + failCount.get());
        System.out.println("소요 시간: " + duration + "ms");
        System.out.println("평균 TPS: " + (threadCount * 1000 / duration));

        String remainingStock = redisTemplate.opsForValue().get("coupon:stock:" + couponId);
        System.out.println("남은 재고 (Redis): " + remainingStock);
        System.out.println("======================================================");

        // 정확히 500명만 성공해야 함
        assertThat(successCount.get()).isEqualTo(stock);
        assertThat(failCount.get()).isEqualTo(threadCount - stock);
        assertThat(Integer.parseInt(remainingStock)).isEqualTo(0);

        // 큐 사이즈 확인
        Long queueSize = redisTemplate.opsForList().size("coupon:issue:queue:" + couponId);
        assertThat(queueSize).isEqualTo(stock);

        // 중복 방지 Set 확인
        Long issuedCount = redisTemplate.opsForSet().size("coupon:issued:" + couponId);
        assertThat(issuedCount).isEqualTo(stock);
    }

    @Test
    @DisplayName("재고 1개 남았을 때 10명이 동시 요청 - 1명만 성공")
    void lastStockConcurrency() throws InterruptedException {
        // given
        int stock = 1;
        int threadCount = 10;
        CouponEntity coupon = createTestCoupon(stock);
        Long couponId = coupon.getId();

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 10명이 동시에 마지막 재고 발급 시도
        for (long userId = 1; userId <= threadCount; userId++) {
            long finalUserId = userId;
            executorService.submit(() -> {
                try {
                    issueCouponUseCase.execute(finalUserId, couponId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        System.out.println("========== 마지막 재고 경합 테스트 결과 ==========");
        System.out.println("초기 재고: " + stock);
        System.out.println("요청 수: " + threadCount);
        System.out.println("성공 수: " + successCount.get());
        System.out.println("실패 수: " + failCount.get());
        System.out.println("=============================================");

        // 정확히 1명만 성공해야 함
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(9);

        String remainingStock = redisTemplate.opsForValue().get("coupon:stock:" + couponId);
        assertThat(Integer.parseInt(remainingStock)).isEqualTo(0);
    }

    // Helper method
    private CouponEntity createTestCoupon(int stock) {
        long now = System.currentTimeMillis();

        CouponEntity coupon = new CouponEntity(
            0L,
            "대규모 테스트 쿠폰 " + System.currentTimeMillis(),
            DiscountType.AMOUNT,
            5000,
            10000,
            50000,
            stock,
            now - 86400000L,  // 어제부터
            now + 86400000L,  // 내일까지
            null,
            null
        );

        CouponEntity savedCoupon = couponRepository.save(coupon);

        // Redis에 재고 초기화
        String stockKey = "coupon:stock:" + savedCoupon.getId();
        redisTemplate.opsForValue().set(stockKey, String.valueOf(savedCoupon.getStock()));

        return savedCoupon;
    }
}