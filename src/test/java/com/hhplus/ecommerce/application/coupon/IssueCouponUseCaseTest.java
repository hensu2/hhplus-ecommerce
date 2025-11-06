package com.hhplus.ecommerce.application.coupon;

import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;
import com.hhplus.ecommerce.domain.coupon.CouponStatus;
import com.hhplus.ecommerce.domain.coupon.DiscountType;
import com.hhplus.ecommerce.infrastructure.coupon.CouponRepository;
import com.hhplus.ecommerce.presentation.coupon.res.IssueCouponResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IssueCouponUseCaseTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private IssueCouponUseCase issueCouponUseCase;

    private CouponEntity testCoupon;
    private long userId;
    private long couponId;

    @BeforeEach
    void setUp() {
        long now = System.currentTimeMillis();
        userId = 1L;
        couponId = 1L;

        testCoupon = new CouponEntity(
            couponId,
            "신규 회원 10% 할인 쿠폰",
            DiscountType.PERCENT,
            10,
            10000,
            5000,
            100,
            now,
            now + 86400000L * 30,
            now,
            now
        );
    }

    @Test
    @DisplayName("쿠폰 발급에 성공한다")
    void issueCoupon() {
        // given
        CouponEntity decreasedCoupon = testCoupon.decreaseStock();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(testCoupon));
        when(couponRepository.findHistoryByUserIdAndCouponId(userId, couponId)).thenReturn(Optional.empty());
        when(couponRepository.decreaseStock(couponId)).thenReturn(decreasedCoupon);

        long now = System.currentTimeMillis();
        CouponHistoryEntity savedHistory = new CouponHistoryEntity(
            1L,
            userId,
            couponId,
            CouponStatus.ISSUED,
            now,
            null
        );
        when(couponRepository.saveHistory(any(CouponHistoryEntity.class))).thenReturn(savedHistory);

        // when
        IssueCouponResponse response = issueCouponUseCase.execute(userId, couponId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCouponId()).isEqualTo(couponId);
        assertThat(response.getCouponName()).isEqualTo("신규 회원 10% 할인 쿠폰");
        assertThat(response.getDiscountType()).isEqualTo("PERCENT");
        assertThat(response.getDiscountAmount()).isEqualTo(10);
        assertThat(response.getStatus()).isEqualTo("ISSUED");
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 ID로 발급 시도 시 예외를 발생시킨다")
    void issueCouponWithInvalidCouponId() {
        // given
        when(couponRepository.findById(couponId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> issueCouponUseCase.execute(userId, couponId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("존재하지 않는 쿠폰입니다.");
    }

    @Test
    @DisplayName("이미 발급받은 쿠폰을 재발급 시도 시 예외를 발생시킨다")
    void issueCouponAlreadyIssued() {
        // given
        long now = System.currentTimeMillis();
        CouponHistoryEntity existingHistory = new CouponHistoryEntity(
            1L,
            userId,
            couponId,
            CouponStatus.ISSUED,
            now,
            null
        );

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(testCoupon));
        when(couponRepository.findHistoryByUserIdAndCouponId(userId, couponId))
            .thenReturn(Optional.of(existingHistory));

        // when & then
        assertThatThrownBy(() -> issueCouponUseCase.execute(userId, couponId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("이미 발급받은 쿠폰입니다.");
    }

    @Test
    @DisplayName("재고가 0인 쿠폰 발급 시도 시 예외를 발생시킨다")
    void issueCouponWithOutOfStock() {
        // given
        long now = System.currentTimeMillis();
        CouponEntity outOfStockCoupon = new CouponEntity(
            couponId,
            "신규 회원 10% 할인 쿠폰",
            DiscountType.PERCENT,
            10,
            10000,
            5000,
            0,
            now,
            now + 86400000L * 30,
            now,
            now
        );

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(outOfStockCoupon));
        when(couponRepository.findHistoryByUserIdAndCouponId(userId, couponId)).thenReturn(Optional.empty());
        when(couponRepository.decreaseStock(couponId)).thenThrow(new IllegalStateException("쿠폰 재고가 부족합니다."));

        // when & then
        assertThatThrownBy(() -> issueCouponUseCase.execute(userId, couponId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("쿠폰 재고가 부족합니다.");
    }

    @Test
    @DisplayName("쿠폰 발급 시 재고가 1 감소한다")
    void issueCouponDecreasesStock() {
        // given
        CouponEntity decreasedCoupon = testCoupon.decreaseStock();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(testCoupon));
        when(couponRepository.findHistoryByUserIdAndCouponId(userId, couponId)).thenReturn(Optional.empty());
        when(couponRepository.decreaseStock(couponId)).thenReturn(decreasedCoupon);

        long now = System.currentTimeMillis();
        CouponHistoryEntity savedHistory = new CouponHistoryEntity(
            1L,
            userId,
            couponId,
            CouponStatus.ISSUED,
            now,
            null
        );
        when(couponRepository.saveHistory(any(CouponHistoryEntity.class))).thenReturn(savedHistory);

        // when
        IssueCouponResponse response = issueCouponUseCase.execute(userId, couponId);

        // then
        assertThat(response).isNotNull();
        verify(couponRepository).decreaseStock(couponId);
    }

    @Test
    @DisplayName("다른 사용자는 같은 쿠폰을 발급받을 수 있다")
    void issueCouponToDifferentUser() {
        // given
        long anotherUserId = 2L;
        CouponEntity decreasedCoupon = testCoupon.decreaseStock();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(testCoupon));
        when(couponRepository.findHistoryByUserIdAndCouponId(anotherUserId, couponId))
            .thenReturn(Optional.empty());
        when(couponRepository.decreaseStock(couponId)).thenReturn(decreasedCoupon);

        long now = System.currentTimeMillis();
        CouponHistoryEntity savedHistory = new CouponHistoryEntity(
            1L,
            anotherUserId,
            couponId,
            CouponStatus.ISSUED,
            now,
            null
        );
        when(couponRepository.saveHistory(any(CouponHistoryEntity.class))).thenReturn(savedHistory);

        // when
        IssueCouponResponse response = issueCouponUseCase.execute(anotherUserId, couponId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
    }
}
