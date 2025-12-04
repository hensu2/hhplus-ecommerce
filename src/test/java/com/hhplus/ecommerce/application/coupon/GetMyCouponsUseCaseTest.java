package com.hhplus.ecommerce.application.coupon;

import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;
import com.hhplus.ecommerce.domain.coupon.CouponStatus;
import com.hhplus.ecommerce.domain.coupon.DiscountType;
import com.hhplus.ecommerce.infrastructure.coupon.CouponRepository;
import com.hhplus.ecommerce.presentation.coupon.res.CouponResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMyCouponsUseCaseTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private GetMyCouponsUseCase getMyCouponsUseCase;

    private long userId;
    private CouponEntity testCoupon1;
    private CouponEntity testCoupon2;
    private CouponHistoryEntity issuedHistory;
    private CouponHistoryEntity usedHistory;

    @BeforeEach
    void setUp() {
        long now = System.currentTimeMillis();
        userId = 1L;

        testCoupon1 = new CouponEntity(
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

        testCoupon2 = new CouponEntity(
            2L,
            "5000원 할인 쿠폰",
            DiscountType.AMOUNT,
            5000,
            20000,
            5000,
            50,
            now,
            now + 86400000L * 30,
            now,
            now
        );

        issuedHistory = new CouponHistoryEntity(
            1L,
            userId,
            1L,
            CouponStatus.ISSUED,
            now,
            null
        );

        usedHistory = new CouponHistoryEntity(
            2L,
            userId,
            2L,
            CouponStatus.USED,
            now,
            now + 3600000L
        );
    }

    @Test
    @DisplayName("사용자의 모든 쿠폰을 조회한다")
    void getMyCoupons() {
        // given
        when(couponRepository.findHistoriesByUserId(userId))
            .thenReturn(Arrays.asList(issuedHistory, usedHistory));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(testCoupon1));
        when(couponRepository.findById(2L)).thenReturn(Optional.of(testCoupon2));

        // when
        List<CouponResponse> result = getMyCouponsUseCase.execute(userId, null);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).couponId()).isEqualTo(1L);
        assertThat(result.get(0).couponName()).isEqualTo("신규 회원 10% 할인 쿠폰");
        assertThat(result.get(0).status()).isEqualTo("ISSUED");
        assertThat(result.get(1).couponId()).isEqualTo(2L);
        assertThat(result.get(1).status()).isEqualTo("USED");
    }

    @Test
    @DisplayName("발급된 쿠폰만 조회한다")
    void getMyCouponsWithIssuedStatus() {
        // given
        when(couponRepository.findHistoriesByUserId(userId))
            .thenReturn(Arrays.asList(issuedHistory, usedHistory));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(testCoupon1));

        // when
        List<CouponResponse> result = getMyCouponsUseCase.execute(userId, CouponStatus.ISSUED);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo("ISSUED");
        assertThat(result.get(0).couponName()).isEqualTo("신규 회원 10% 할인 쿠폰");
    }

    @Test
    @DisplayName("사용된 쿠폰만 조회한다")
    void getMyCouponsWithUsedStatus() {
        // given
        when(couponRepository.findHistoriesByUserId(userId))
            .thenReturn(Arrays.asList(issuedHistory, usedHistory));
        when(couponRepository.findById(2L)).thenReturn(Optional.of(testCoupon2));

        // when
        List<CouponResponse> result = getMyCouponsUseCase.execute(userId, CouponStatus.USED);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo("USED");
        assertThat(result.get(0).couponName()).isEqualTo("5000원 할인 쿠폰");
        assertThat(result.get(0).usedAt()).isNotNull();
    }

    @Test
    @DisplayName("발급받은 쿠폰이 없으면 빈 리스트를 반환한다")
    void getMyCouponsWithNoHistory() {
        // given
        when(couponRepository.findHistoriesByUserId(userId)).thenReturn(List.of());

        // when
        List<CouponResponse> result = getMyCouponsUseCase.execute(userId, null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("쿠폰 응답에 모든 필드가 올바르게 매핑된다")
    void getMyCouponsResponseFields() {
        // given
        when(couponRepository.findHistoriesByUserId(userId))
            .thenReturn(Arrays.asList(issuedHistory));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(testCoupon1));

        // when
        List<CouponResponse> result = getMyCouponsUseCase.execute(userId, null);

        // then
        assertThat(result).hasSize(1);
        CouponResponse response = result.get(0);
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.couponId()).isEqualTo(1L);
        assertThat(response.couponName()).isEqualTo("신규 회원 10% 할인 쿠폰");
        assertThat(response.discountType()).isEqualTo("PERCENT");
        assertThat(response.discountAmount()).isEqualTo(10);
        assertThat(response.useMinAmount()).isEqualTo(10000);
        assertThat(response.useMaxAmount()).isEqualTo(5000);
        assertThat(response.status()).isEqualTo("ISSUED");
        assertThat(response.issuedAt()).isNotNull();
        assertThat(response.usedAt()).isNull();
    }
}
