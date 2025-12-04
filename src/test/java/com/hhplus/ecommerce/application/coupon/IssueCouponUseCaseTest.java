package com.hhplus.ecommerce.application.coupon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.DiscountType;
import com.hhplus.ecommerce.infrastructure.coupon.CouponRepository;
import com.hhplus.ecommerce.presentation.coupon.res.IssueCouponResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@DisplayName("Redis 기반 쿠폰 발급 단위 테스트")
class IssueCouponUseCaseTest {

    @Autowired
    private IssueCouponUseCase issueCouponUseCase;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Redis 초기화 - 모든 쿠폰 관련 키 삭제
        redisTemplate.keys("coupon:*").forEach(key -> redisTemplate.delete(key));
    }

    @Test
    @DisplayName("정상 발급 - 재고 차감 및 큐 추가")
    void issueSuccess() {
        // given
        long userId = 123L;
        CouponEntity coupon = createTestCoupon(100);
        Long couponId = coupon.getId();

        // when
        IssueCouponResponse response = issueCouponUseCase.execute(userId, couponId);

        // then
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getMessage()).isEqualTo("쿠폰 발급 요청이 접수되었습니다.");
        assertThat(response.getCouponId()).isEqualTo(couponId);
        assertThat(response.getUserId()).isEqualTo(userId);

        // 재고 확인
        String stock = redisTemplate.opsForValue().get("coupon:stock:" + couponId);
        assertThat(stock).isEqualTo("99");

        // 큐 확인
        Long queueSize = redisTemplate.opsForList().size("coupon:issue:queue:" + couponId);
        assertThat(queueSize).isEqualTo(1);

        // 중복 방지 Set 확인
        Boolean isMember = redisTemplate.opsForSet()
            .isMember("coupon:issued:" + couponId, String.valueOf(userId));
        assertThat(isMember).isTrue();
    }

    @Test
    @DisplayName("중복 발급 방지")
    void preventDuplicateIssue() {
        // given
        long userId = 123L;
        CouponEntity coupon = createTestCoupon(100);
        Long couponId = coupon.getId();

        // 첫 번째 발급
        issueCouponUseCase.execute(userId, couponId);

        // when & then - 두 번째 발급 시도
        assertThatThrownBy(() -> issueCouponUseCase.execute(userId, couponId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("이미 발급받은 쿠폰입니다.");

        // 재고는 한 번만 차감되어야 함
        String stock = redisTemplate.opsForValue().get("coupon:stock:" + couponId);
        assertThat(stock).isEqualTo("99");
    }

    @Test
    @DisplayName("재고 소진 시 예외 발생")
    void stockExhausted() {
        // given
        long userId = 123L;
        CouponEntity coupon = createTestCoupon(0); // 재고 0
        Long couponId = coupon.getId();

        // when & then
        assertThatThrownBy(() -> issueCouponUseCase.execute(userId, couponId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("쿠폰이 모두 소진되었습니다.");

        // 큐에 추가되지 않아야 함
        Long queueSize = redisTemplate.opsForList().size("coupon:issue:queue:" + couponId);
        assertThat(queueSize).isEqualTo(0);
    }

    @Test
    @DisplayName("유효 기간이 아닌 쿠폰 - 시작 전")
    void notValidYet() {
        // given
        long userId = 123L;
        long now = System.currentTimeMillis();
        CouponEntity coupon = createTestCouponWithPeriod(
            100,
            now + 86400000L, // 내일 시작
            now + 172800000L  // 모레 종료
        );
        Long couponId = coupon.getId();

        // when & then
        assertThatThrownBy(() -> issueCouponUseCase.execute(userId, couponId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("쿠폰 유효 기간이 아닙니다.");
    }

    @Test
    @DisplayName("유효 기간이 아닌 쿠폰 - 종료 후")
    void expired() {
        // given
        long userId = 123L;
        long now = System.currentTimeMillis();
        CouponEntity coupon = createTestCouponWithPeriod(
            100,
            now - 172800000L, // 이틀 전 시작
            now - 86400000L   // 어제 종료
        );
        Long couponId = coupon.getId();

        // when & then
        assertThatThrownBy(() -> issueCouponUseCase.execute(userId, couponId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("쿠폰 유효 기간이 아닙니다.");
    }

    @Test
    @DisplayName("여러 사용자가 순차적으로 발급")
    void multipleUsersSequential() {
        // given
        CouponEntity coupon = createTestCoupon(10);
        Long couponId = coupon.getId();

        // when - 10명이 순차적으로 발급
        for (long userId = 1; userId <= 10; userId++) {
            IssueCouponResponse response = issueCouponUseCase.execute(userId, couponId);
            assertThat(response.getStatus()).isEqualTo("PENDING");
        }

        // then
        String stock = redisTemplate.opsForValue().get("coupon:stock:" + couponId);
        assertThat(stock).isEqualTo("0");

        Long queueSize = redisTemplate.opsForList().size("coupon:issue:queue:" + couponId);
        assertThat(queueSize).isEqualTo(10);

        Long issuedCount = redisTemplate.opsForSet().size("coupon:issued:" + couponId);
        assertThat(issuedCount).isEqualTo(10);
    }

    // Helper methods
    private CouponEntity createTestCoupon(int stock) {
        long now = System.currentTimeMillis();
        return createTestCouponWithPeriod(
            stock,
            now - 86400000L,  // 어제부터
            now + 86400000L   // 내일까지
        );
    }

    private CouponEntity createTestCouponWithPeriod(int stock, long validFrom, long validUntil) {
        CouponEntity coupon = new CouponEntity(
            0L,
            "테스트 쿠폰 " + System.currentTimeMillis(),
            DiscountType.AMOUNT,
            5000,
            10000,
            50000,
            stock,
            validFrom,
            validUntil,
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
