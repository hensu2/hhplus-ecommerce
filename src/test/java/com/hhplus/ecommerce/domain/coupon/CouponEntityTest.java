package com.hhplus.ecommerce.domain.coupon;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponEntityTest {

    private CouponEntity testCoupon;

    @BeforeEach
    void setUp() {
        long now = System.currentTimeMillis();
        testCoupon = new CouponEntity(
            1L,
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
    @DisplayName("재고를 1 감소시킬 수 있다")
    void decreaseStock_Success() {
        // when
        CouponEntity updated = testCoupon.decreaseStock();

        // then
        assertThat(updated.getStock()).isEqualTo(99);
        assertThat(updated.getId()).isEqualTo(testCoupon.getId());
        assertThat(updated.getCouponName()).isEqualTo(testCoupon.getCouponName());
    }

    @Test
    @DisplayName("재고가 0일 때 감소 시도 시 예외가 발생한다")
    void decreaseStock_WithZeroStock_ThrowsException() {
        // given
        long now = System.currentTimeMillis();
        CouponEntity zeroStockCoupon = new CouponEntity(
            1L,
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

        // when & then
        assertThatThrownBy(zeroStockCoupon::decreaseStock)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("쿠폰 재고가 부족합니다.");
    }

    @Test
    @DisplayName("재고를 정확히 0으로 감소시킬 수 있다")
    void decreaseStock_ToZero() {
        // given
        long now = System.currentTimeMillis();
        CouponEntity oneStockCoupon = new CouponEntity(
            1L,
            "신규 회원 10% 할인 쿠폰",
            DiscountType.PERCENT,
            10,
            10000,
            5000,
            1,
            now,
            now + 86400000L * 30,
            now,
            now
        );

        // when
        CouponEntity updated = oneStockCoupon.decreaseStock();

        // then
        assertThat(updated.getStock()).isEqualTo(0);
    }

    @Test
    @DisplayName("재고 감소 시 updatedAt이 업데이트된다")
    void decreaseStock_UpdatesTimestamp() throws InterruptedException {
        // given
        long beforeUpdate = testCoupon.getUpdatedAt();
        Thread.sleep(10);

        // when
        CouponEntity updated = testCoupon.decreaseStock();

        // then
        assertThat(updated.getUpdatedAt()).isGreaterThan(beforeUpdate);
    }

    @Test
    @DisplayName("재고 감소 후에도 다른 필드는 유지된다")
    void decreaseStock_PreservesOtherFields() {
        // when
        CouponEntity updated = testCoupon.decreaseStock();

        // then
        assertThat(updated.getId()).isEqualTo(testCoupon.getId());
        assertThat(updated.getCouponName()).isEqualTo(testCoupon.getCouponName());
        assertThat(updated.getDiscountType()).isEqualTo(testCoupon.getDiscountType());
        assertThat(updated.getDiscountAmount()).isEqualTo(testCoupon.getDiscountAmount());
        assertThat(updated.getUseMinAmount()).isEqualTo(testCoupon.getUseMinAmount());
        assertThat(updated.getUseMaxAmount()).isEqualTo(testCoupon.getUseMaxAmount());
        assertThat(updated.getValidFrom()).isEqualTo(testCoupon.getValidFrom());
        assertThat(updated.getValidUntil()).isEqualTo(testCoupon.getValidUntil());
        assertThat(updated.getCreatedAt()).isEqualTo(testCoupon.getCreatedAt());
    }

    @Test
    @DisplayName("여러 번 재고를 감소시킬 수 있다")
    void decreaseStock_Multiple() {
        // when
        CouponEntity updated1 = testCoupon.decreaseStock();
        CouponEntity updated2 = updated1.decreaseStock();
        CouponEntity updated3 = updated2.decreaseStock();

        // then
        assertThat(updated3.getStock()).isEqualTo(97);
    }
}
