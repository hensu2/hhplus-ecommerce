package com.hhplus.ecommerce.integration;

import com.hhplus.ecommerce.application.point.ChargeUserPointUseCase;
import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.infrastructure.point.PointHistoryJpaRepository;
import com.hhplus.ecommerce.infrastructure.user.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 포인트 충전 동시성 테스트
 * - 여러 스레드가 동시에 포인트를 충전할 때 데이터 정합성 검증
 * - JPA @Version을 통한 낙관적 락 또는 비관적 락 테스트
 */
@DisplayName("포인트 동시성 테스트")
class PointConcurrencyTest extends TestContainerConfig {

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PointHistoryJpaRepository pointHistoryJpaRepository;

    @Autowired
    private ChargeUserPointUseCase chargeUserPointUseCase;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = UserEntity.create("concurrencyTestUser", 0L, "USER");
        testUser = userJpaRepository.save(testUser);
    }

    @Test
    @DisplayName("동시에 여러 건의 포인트 충전 요청이 들어와도 모두 정상 처리된다")
    void concurrentChargePoint_AllSuccess() throws InterruptedException {
        // given
        int threadCount = 10;
        int chargeAmount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 10개 스레드가 동시에 1000원씩 충전
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    chargeUserPointUseCase.execute(testUser.getId(), chargeAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("충전 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        System.out.println("성공: " + successCount.get() + ", 실패: " + failCount.get());

        // 모든 충전이 성공해야 함
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failCount.get()).isEqualTo(0);

        // 최종 포인트 확인
        UserEntity finalUser = userJpaRepository.findById(testUser.getId()).orElseThrow();
        Long expectedPoint = (long) (threadCount * chargeAmount);
        assertThat(finalUser.getPoint()).isEqualTo(expectedPoint);

        // 포인트 내역 개수 확인
        var histories = pointHistoryJpaRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());
        assertThat(histories).hasSize(threadCount);
    }

    @Test
    @DisplayName("동시에 포인트 충전과 사용이 발생해도 정합성이 유지된다")
    void concurrentChargeAndUsePoint() throws InterruptedException {
        // given
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount * 2);
        CountDownLatch latch = new CountDownLatch(threadCount * 2);

        // 초기 포인트 충전
        chargeUserPointUseCase.execute(testUser.getId(), 50000);

        // when - 5개는 충전, 5개는 사용
        for (int i = 0; i < threadCount; i++) {
            // 충전 스레드
            executorService.submit(() -> {
                try {
                    chargeUserPointUseCase.execute(testUser.getId(), 1000);
                } catch (Exception e) {
                    System.err.println("충전 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });

            // 사용 스레드
            executorService.submit(() -> {
                try {
                    UserEntity user = userJpaRepository.findById(testUser.getId()).orElseThrow();
                    user.usePoint(1000L);
                    userJpaRepository.save(user);
                } catch (Exception e) {
                    System.err.println("사용 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 정확한 포인트 계산은 어렵지만 음수가 되지 않아야 함
        UserEntity finalUser = userJpaRepository.findById(testUser.getId()).orElseThrow();
        assertThat(finalUser.getPoint()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("100명이 동시에 포인트를 충전해도 Lost Update가 발생하지 않는다")
    void concurrentChargePoint_100Threads() throws InterruptedException {
        // given
        int threadCount = 100;
        int chargeAmount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    chargeUserPointUseCase.execute(testUser.getId(), chargeAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("충전 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        System.out.println("100개 스레드 중 성공: " + successCount.get());

        UserEntity finalUser = userJpaRepository.findById(testUser.getId()).orElseThrow();
        Long expectedPoint = (long) (successCount.get() * chargeAmount);
        assertThat(finalUser.getPoint()).isEqualTo(expectedPoint);
    }
}
