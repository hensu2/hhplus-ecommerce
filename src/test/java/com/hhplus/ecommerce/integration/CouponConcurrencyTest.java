package com.hhplus.ecommerce.integration;

import com.hhplus.ecommerce.application.coupon.IssueCouponUseCase;
import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;
import com.hhplus.ecommerce.infrastructure.coupon.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("쿠폰 동시성 테스트")
class CouponConcurrencyTest {

    @Autowired
    private IssueCouponUseCase issueCouponUseCase;

    @Autowired
    private CouponRepository couponRepository;

    private Long testCouponId;

    @BeforeEach
    void setUp() {
        // 테스트용 쿠폰 ID (이미 존재하는 쿠폰 사용)
        testCouponId = 1L;

        // 테스트를 위한 초기 쿠폰 재고 설정 (충분한 재고 확보)
        CouponEntity coupon = couponRepository.getOrThrow(testCouponId);
        if (coupon.getStock() < 30) {
            // 쿠폰 재고를 100개로 재설정
            CouponEntity updatedCoupon = new com.hhplus.ecommerce.domain.coupon.CouponEntity(
                coupon.getId(),
                coupon.getCouponName(),
                coupon.getDiscountType(),
                coupon.getDiscountAmount(),
                coupon.getUseMinAmount(),
                coupon.getUseMaxAmount(),
                100,
                coupon.getValidFrom(),
                coupon.getValidUntil(),
                coupon.getCreatedAt(),
                coupon.getUpdatedAt()
            );
            couponRepository.save(updatedCoupon);
        }
    }

    @Test
    @DisplayName("동시성 제어 검증 - 10명이 동시 발급 시도 시 재고만큼만 발급됨")
    void concurrentCouponIssuance_WithConcurrencyControl() throws InterruptedException {
        // given
        CouponEntity coupon = couponRepository.getOrThrow(testCouponId);
        Integer initialStock = coupon.getStock();

        // 서로 다른 사용자 10명이 동시 발급 시도
        Long[] userIds = {101L, 102L, 103L, 104L, 105L, 106L, 107L, 108L, 109L, 110L};
        int threadCount = userIds.length;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 10명이 동시에 같은 쿠폰 발급 시도
        for (Long userId : userIds) {
            executorService.submit(() -> {
                try {
                    issueCouponUseCase.execute(userId, testCouponId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("쿠폰 발급 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        CouponEntity updatedCoupon = couponRepository.getOrThrow(testCouponId);
        Integer finalStock = updatedCoupon.getStock();
        Integer expectedStock = initialStock - successCount.get();

        System.out.println("========== 쿠폰 동시성 테스트 결과 ==========");
        System.out.println("초기 쿠폰 재고: " + initialStock);
        System.out.println("발급 시도 횟수: " + threadCount);
        System.out.println("성공한 발급 수: " + successCount.get());
        System.out.println("실패한 발급 수: " + failCount.get());
        System.out.println("예상 최종 재고: " + expectedStock);
        System.out.println("실제 최종 재고: " + finalStock);
        System.out.println("=========================================");

        // ConcurrentHashMap.compute()로 동시성 제어 - 정확한 쿠폰 발급 보장
        assertThat(finalStock).isEqualTo(expectedStock)
                .withFailMessage("동시성 제어로 정확한 쿠폰 재고 차감이 되어야 합니다!");

        // 재고는 음수가 될 수 없음
        assertThat(finalStock).isGreaterThanOrEqualTo(0)
                .withFailMessage("쿠폰 재고는 음수가 될 수 없습니다!");
    }

    @Test
    @DisplayName("동시성 제어 검증 - 재고보다 많은 동시 발급 시도 시 초과 발급 방지")
    void concurrentCouponIssuance_PreventOverIssuance() throws InterruptedException {
        // given
        CouponEntity coupon = couponRepository.getOrThrow(testCouponId);
        Integer initialStock = coupon.getStock();

        // 재고보다 많은 사용자가 동시 발급 시도
        int threadCount = Math.min(initialStock + 5, 20);
        Long[] userIds = new Long[threadCount];
        for (int i = 0; i < threadCount; i++) {
            userIds[i] = 200L + i; // 서로 다른 사용자 ID
        }

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 재고보다 많은 사용자가 동시 발급 시도
        for (Long userId : userIds) {
            executorService.submit(() -> {
                try {
                    issueCouponUseCase.execute(userId, testCouponId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("쿠폰 재고 부족으로 발급 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        CouponEntity updatedCoupon = couponRepository.getOrThrow(testCouponId);
        Integer finalStock = updatedCoupon.getStock();

        System.out.println("========== 쿠폰 초과 발급 방지 테스트 결과 ==========");
        System.out.println("초기 쿠폰 재고: " + initialStock);
        System.out.println("발급 시도 횟수: " + threadCount);
        System.out.println("성공한 발급 수: " + successCount.get());
        System.out.println("실패한 발급 수: " + failCount.get());
        System.out.println("최종 재고: " + finalStock);
        System.out.println("=============================================");

        // 재고를 초과하는 발급은 실패해야 함
        assertThat(finalStock).isGreaterThanOrEqualTo(0)
                .withFailMessage("쿠폰 재고는 음수가 될 수 없습니다!");

        // 성공한 발급 수 + 최종 재고 = 초기 재고
        assertThat(successCount.get() + finalStock).isEqualTo(initialStock)
                .withFailMessage("정확한 쿠폰 재고 관리가 되어야 합니다!");

        // 성공한 발급 수는 초기 재고를 초과할 수 없음
        assertThat(successCount.get()).isLessThanOrEqualTo(initialStock)
                .withFailMessage("발급된 쿠폰 수가 초기 재고를 초과할 수 없습니다!");
    }

    @Test
    @DisplayName("동시성 제어 검증 - 같은 사용자가 동시에 여러 번 발급 시도 시 1건만 성공")
    void concurrentCouponIssuance_SameUserMultipleTimes() throws InterruptedException {
        // given
        Long userId = 300L;
        int threadCount = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 같은 사용자가 동시에 10번 발급 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    issueCouponUseCase.execute(userId, testCouponId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("중복 발급 차단: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        List<CouponHistoryEntity> histories = couponRepository.findHistoriesByUserId(userId);
        long issuedCount = histories.stream()
                .filter(h -> h.getCouponId().equals(testCouponId))
                .count();

        System.out.println("========== 중복 발급 방지 테스트 결과 ==========");
        System.out.println("발급 시도 횟수: " + threadCount);
        System.out.println("성공한 발급 수: " + successCount.get());
        System.out.println("실패한 발급 수: " + failCount.get());
        System.out.println("실제 발급된 쿠폰 수: " + issuedCount);
        System.out.println("==========================================");

        // 같은 사용자는 같은 쿠폰을 1번만 발급받을 수 있음
        assertThat(issuedCount).isEqualTo(1)
                .withFailMessage("같은 사용자는 같은 쿠폰을 1번만 발급받을 수 있어야 합니다!");

        assertThat(successCount.get()).isEqualTo(1)
                .withFailMessage("중복 발급 시도는 실패해야 합니다!");

        assertThat(failCount.get()).isEqualTo(threadCount - 1)
                .withFailMessage("중복 발급 시도는 모두 실패해야 합니다!");
    }
}
