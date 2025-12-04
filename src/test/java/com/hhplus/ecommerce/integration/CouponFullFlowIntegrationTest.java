package com.hhplus.ecommerce.integration;

import com.hhplus.ecommerce.application.coupon.IssueCouponUseCase;
import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;
import com.hhplus.ecommerce.domain.coupon.DiscountType;
import com.hhplus.ecommerce.infrastructure.coupon.CouponRepository;
import com.hhplus.ecommerce.presentation.coupon.res.IssueCouponResponse;
import com.hhplus.ecommerce.scheduler.CouponSyncScheduler;
import com.hhplus.ecommerce.worker.CouponIssueWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@DisplayName("쿠폰 발급 전체 플로우 통합 테스트")
class CouponFullFlowIntegrationTest {

    @Autowired
    private IssueCouponUseCase issueCouponUseCase;

    @Autowired
    private CouponIssueWorker couponIssueWorker;

    @Autowired
    private CouponSyncScheduler couponSyncScheduler;

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
    @DisplayName("전체 플로우 - 발급 요청 → 워커 처리 → DB 동기화")
    void fullFlowTest() {
        // given
        long userId = 123L;
        CouponEntity coupon = createTestCoupon(10);
        Long couponId = coupon.getId();

        // when - 1. 발급 요청
        IssueCouponResponse response = issueCouponUseCase.execute(userId, couponId);

        // then - 1. 즉시 응답 확인
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getMessage()).isEqualTo("쿠폰 발급 요청이 접수되었습니다.");

        // 큐에 추가되었는지 확인
        Long queueSize = redisTemplate.opsForList().size("coupon:issue:queue:" + couponId);
        assertThat(queueSize).isEqualTo(1);

        // when - 2. 워커가 처리 (수동 실행)
        couponIssueWorker.processIssueQueue();

        // then - 2. Redis Hash에 임시 저장되었는지 확인
        await()
            .atMost(3, TimeUnit.SECONDS)
            .until(() -> {
                String pendingKey = "coupon:issued:pending:" + couponId;
                return redisTemplate.opsForHash().hasKey(pendingKey, String.valueOf(userId));
            });

        String pendingKey = "coupon:issued:pending:" + couponId;
        assertThat(redisTemplate.opsForHash().hasKey(pendingKey, String.valueOf(userId))).isTrue();

        // 큐는 비워져야 함
        queueSize = redisTemplate.opsForList().size("coupon:issue:queue:" + couponId);
        assertThat(queueSize).isEqualTo(0);

        // when - 3. 스케줄러가 DB 동기화 (수동 실행)
        couponSyncScheduler.syncCouponIssuesToDB();

        // then - 3. DB에 저장되었는지 확인
        await()
            .atMost(3, TimeUnit.SECONDS)
            .until(() -> couponRepository.findHistoryByUserIdAndCouponId(userId, couponId).isPresent());

        Optional<CouponHistoryEntity> history = couponRepository
            .findHistoryByUserIdAndCouponId(userId, couponId);

        assertThat(history).isPresent();
        assertThat(history.get().getUserId()).isEqualTo(userId);
        assertThat(history.get().getCouponId()).isEqualTo(couponId);
        assertThat(history.get().getStatus().name()).isEqualTo("ISSUED");

        // Redis pending Hash는 삭제되어야 함
        assertThat(redisTemplate.opsForHash().hasKey(pendingKey, String.valueOf(userId))).isFalse();

        // 동기화 완료 마커 확인
        String syncKey = "coupon:synced:histories";
        String syncValue = userId + ":" + couponId;
        assertThat(redisTemplate.opsForSet().isMember(syncKey, syncValue)).isTrue();

        System.out.println("========== 전체 플로우 테스트 성공 ==========");
        System.out.println("1. 발급 요청 → PENDING 응답 ✓");
        System.out.println("2. 워커 처리 → Redis Hash 임시 저장 ✓");
        System.out.println("3. 스케줄러 동기화 → DB 저장 완료 ✓");
        System.out.println("==========================================");
    }

    @Test
    @DisplayName("여러 사용자 전체 플로우 - 5명 동시 발급")
    void multipleUsersFullFlow() {
        // given
        CouponEntity coupon = createTestCoupon(10);
        Long couponId = coupon.getId();
        long[] userIds = {101L, 102L, 103L, 104L, 105L};

        // when - 1. 5명이 동시 발급 요청
        for (long userId : userIds) {
            IssueCouponResponse response = issueCouponUseCase.execute(userId, couponId);
            assertThat(response.getStatus()).isEqualTo("PENDING");
        }

        // then - 1. 큐에 5개 요청이 있어야 함
        Long queueSize = redisTemplate.opsForList().size("coupon:issue:queue:" + couponId);
        assertThat(queueSize).isEqualTo(5);

        // when - 2. 워커가 모두 처리 (여러 번 실행)
        for (int i = 0; i < 5; i++) {
            couponIssueWorker.processIssueQueue();
        }

        // then - 2. Redis Hash에 5개 모두 임시 저장
        await()
            .atMost(5, TimeUnit.SECONDS)
            .until(() -> {
                String pendingKey = "coupon:issued:pending:" + couponId;
                Long size = redisTemplate.opsForHash().size(pendingKey);
                return size != null && size == 5;
            });

        // when - 3. 스케줄러가 DB 동기화
        couponSyncScheduler.syncCouponIssuesToDB();

        // then - 3. DB에 5개 모두 저장되었는지 확인
        await()
            .atMost(5, TimeUnit.SECONDS)
            .until(() -> {
                for (long userId : userIds) {
                    if (couponRepository.findHistoryByUserIdAndCouponId(userId, couponId).isEmpty()) {
                        return false;
                    }
                }
                return true;
            });

        // 모든 사용자의 히스토리 확인
        for (long userId : userIds) {
            Optional<CouponHistoryEntity> history = couponRepository
                .findHistoryByUserIdAndCouponId(userId, couponId);

            assertThat(history).isPresent();
            assertThat(history.get().getUserId()).isEqualTo(userId);
            assertThat(history.get().getCouponId()).isEqualTo(couponId);
        }

        System.out.println("========== 다중 사용자 플로우 테스트 성공 ==========");
        System.out.println("5명 동시 발급 요청 → 워커 처리 → DB 동기화 완료 ✓");
        System.out.println("================================================");
    }

    @Test
    @DisplayName("중복 발급 방지 - 전체 플로우에서 검증")
    void duplicatePreventionFullFlow() {
        // given
        long userId = 999L;
        CouponEntity coupon = createTestCoupon(10);
        Long couponId = coupon.getId();

        // when - 1. 첫 번째 발급
        IssueCouponResponse response1 = issueCouponUseCase.execute(userId, couponId);
        assertThat(response1.getStatus()).isEqualTo("PENDING");

        // when - 2. 두 번째 발급 시도 (Redis Set에 이미 있음)
        assertThatThrownBy(() -> issueCouponUseCase.execute(userId, couponId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("이미 발급받은 쿠폰입니다.");

        // when - 3. 워커 처리 및 DB 동기화
        couponIssueWorker.processIssueQueue();
        couponSyncScheduler.syncCouponIssuesToDB();

        // then - DB에는 1개만 저장되어야 함
        await()
            .atMost(5, TimeUnit.SECONDS)
            .until(() -> couponRepository.findHistoryByUserIdAndCouponId(userId, couponId).isPresent());

        Optional<CouponHistoryEntity> history = couponRepository
            .findHistoryByUserIdAndCouponId(userId, couponId);
        assertThat(history).isPresent();

        // when - 4. DB 동기화 후에도 재발급 시도
        assertThatThrownBy(() -> issueCouponUseCase.execute(userId, couponId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("이미 발급받은 쿠폰입니다.");

        System.out.println("========== 중복 발급 방지 테스트 성공 ==========");
        System.out.println("Redis Set 중복 체크 ✓");
        System.out.println("DB 동기화 후에도 중복 방지 ✓");
        System.out.println("============================================");
    }

    @Test
    @DisplayName("재고 확인 - 전체 플로우에서 재고 정합성")
    void stockConsistencyFullFlow() {
        // given
        int initialStock = 5;
        CouponEntity coupon = createTestCoupon(initialStock);
        Long couponId = coupon.getId();

        // when - 5명이 발급
        for (long userId = 1; userId <= 5; userId++) {
            issueCouponUseCase.execute(userId, couponId);
        }

        // then - Redis 재고 확인
        String redisStock = redisTemplate.opsForValue().get("coupon:stock:" + couponId);
        assertThat(Integer.parseInt(redisStock)).isEqualTo(0);

        // when - 6번째 사용자는 실패해야 함
        assertThatThrownBy(() -> issueCouponUseCase.execute(6L, couponId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("쿠폰이 모두 소진되었습니다.");

        // when - 워커 처리 및 DB 동기화
        for (int i = 0; i < 5; i++) {
            couponIssueWorker.processIssueQueue();
        }
        couponSyncScheduler.syncCouponIssuesToDB();

        // then - DB에 5개만 저장되어야 함
        await()
            .atMost(5, TimeUnit.SECONDS)
            .until(() -> {
                int count = 0;
                for (long userId = 1; userId <= 5; userId++) {
                    if (couponRepository.findHistoryByUserIdAndCouponId(userId, couponId).isPresent()) {
                        count++;
                    }
                }
                return count == 5;
            });

        // 6번째 사용자는 DB에 없어야 함
        Optional<CouponHistoryEntity> history6 = couponRepository
            .findHistoryByUserIdAndCouponId(6L, couponId);
        assertThat(history6).isEmpty();

        System.out.println("========== 재고 정합성 테스트 성공 ==========");
        System.out.println("초기 재고: " + initialStock);
        System.out.println("Redis 재고: 0");
        System.out.println("DB 저장: 5개");
        System.out.println("6번째 요청 차단 ✓");
        System.out.println("=========================================");
    }

    // Helper method
    private CouponEntity createTestCoupon(int stock) {
        long now = System.currentTimeMillis();

        CouponEntity coupon = new CouponEntity(
            0L,
            "플로우 테스트 쿠폰 " + System.currentTimeMillis(),
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