package com.hhplus.ecommerce.integration;

import com.hhplus.ecommerce.application.user.ChargePointUseCase;
import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.infrastructure.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("포인트 동시성 테스트")
class PointConcurrencyTest {

    @Autowired
    private ChargePointUseCase chargePointUseCase;

    @Autowired
    private UserRepository userRepository;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 ID (이미 존재하는 사용자 사용)
        testUserId = 1L;
    }

    @Test
    @DisplayName("동시성 문제 - Lost Update로 인한 포인트 손실")
    void concurrentPointCharge_LostUpdate() throws InterruptedException {
        // given
        UserEntity user = userRepository.getOrThrow(testUserId);
        Long initialPoint = user.getPoint();

        Long chargeAmount = 1000L; // 1회 충전 금액
        int threadCount = 10; // 동시 충전 횟수

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 같은 사용자가 동시에 10번 포인트 충전
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    chargePointUseCase.execute(testUserId, chargeAmount);
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
        UserEntity updatedUser = userRepository.getOrThrow(testUserId);
        Long finalPoint = updatedUser.getPoint();
        Long expectedPoint = initialPoint + (chargeAmount * threadCount);
        Long actualIncrease = finalPoint - initialPoint;

        System.out.println("========== 포인트 동시성 테스트 결과 ==========");
        System.out.println("초기 포인트: " + initialPoint);
        System.out.println("충전 시도 횟수: " + threadCount);
        System.out.println("1회 충전 금액: " + chargeAmount);
        System.out.println("성공한 충전 요청 수: " + successCount.get());
        System.out.println("실패한 충전 요청 수: " + failCount.get());
        System.out.println("예상 최종 포인트: " + expectedPoint);
        System.out.println("실제 최종 포인트: " + finalPoint);
        System.out.println("실제 증가 금액: " + actualIncrease);
        System.out.println("손실 금액: " + (expectedPoint - finalPoint));
        System.out.println("==========================================");

        // 동시성 제어가 없으면 Lost Update 발생 → 포인트 손실!
        assertThat(finalPoint).isLessThan(expectedPoint)
                .withFailMessage("Lost Update 문제로 포인트가 손실되었습니다!");

        // 만약 동시성 제어가 제대로 되어 있다면 예상값과 같아야 함
        // assertThat(finalPoint).isEqualTo(expectedPoint);
    }

    @Test
    @DisplayName("동시성 문제 - 여러 사용자가 동시에 포인트 충전")
    void concurrentMultipleUsers_PointCharge() throws InterruptedException {
        // given
        Long[] userIds = {1L, 2L, 3L};
        Long chargeAmount = 1000L;
        int chargeCountPerUser = 5;
        int totalThreadCount = userIds.length * chargeCountPerUser;

        ExecutorService executorService = Executors.newFixedThreadPool(totalThreadCount);
        CountDownLatch latch = new CountDownLatch(totalThreadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 초기 포인트 기록
        Long[] initialPoints = new Long[userIds.length];
        for (int i = 0; i < userIds.length; i++) {
            UserEntity user = userRepository.getOrThrow(userIds[i]);
            initialPoints[i] = user.getPoint();
        }

        // when - 여러 사용자가 동시에 포인트 충전
        for (Long userId : userIds) {
            for (int i = 0; i < chargeCountPerUser; i++) {
                executorService.submit(() -> {
                    try {
                        chargePointUseCase.execute(userId, chargeAmount);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        latch.await();
        executorService.shutdown();

        // then - 각 사용자별 포인트 검증
        System.out.println("========== 다중 사용자 포인트 동시성 테스트 결과 ==========");
        System.out.println("전체 성공 요청 수: " + successCount.get());
        System.out.println("전체 실패 요청 수: " + failCount.get());

        int lostUpdateCount = 0;
        for (int i = 0; i < userIds.length; i++) {
            UserEntity user = userRepository.getOrThrow(userIds[i]);
            Long expectedPoint = initialPoints[i] + (chargeAmount * chargeCountPerUser);
            Long actualPoint = user.getPoint();
            Long lostAmount = expectedPoint - actualPoint;

            System.out.println("사용자 " + userIds[i] + " - 초기: " + initialPoints[i]
                + ", 예상: " + expectedPoint + ", 실제: " + actualPoint
                + ", 손실: " + lostAmount);

            if (actualPoint < expectedPoint) {
                lostUpdateCount++;
            }
        }

        System.out.println("Lost Update 발생 사용자 수: " + lostUpdateCount);
        System.out.println("====================================================");

        // 동시성 제어가 없으면 최소 1명 이상의 사용자에서 Lost Update 발생
        assertThat(lostUpdateCount).isGreaterThan(0)
                .withFailMessage("동시성 제어가 없어 Lost Update가 발생했습니다!");
    }
}