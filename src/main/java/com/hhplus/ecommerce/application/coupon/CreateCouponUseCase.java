package com.hhplus.ecommerce.application.coupon;

import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.DiscountType;
import com.hhplus.ecommerce.infrastructure.coupon.CouponRepository;
import com.hhplus.ecommerce.presentation.coupon.req.CreateCouponRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateCouponUseCase {

    private final CouponRepository couponRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public CouponEntity execute(CreateCouponRequest request) {
        // 1. 입력 검증
        validateRequest(request);

        // 2. 쿠폰 엔티티 생성
        CouponEntity coupon = new CouponEntity(
            0L,  // ID는 자동 생성
            request.couponName(),
            DiscountType.valueOf(request.discountType()),
            request.discountAmount(),
            request.useMinAmount(),
            request.useMaxAmount(),
            request.stock(),
            request.validFrom(),
            request.validUntil(),
            null,  // createdAt은 자동 생성
            null   // updatedAt은 자동 생성
        );

        // 3. DB 저장
        CouponEntity savedCoupon = couponRepository.save(coupon);

        // 4. Redis에 재고 초기화
        String stockKey = "coupon:stock:" + savedCoupon.getId();
        redisTemplate.opsForValue().set(stockKey, String.valueOf(savedCoupon.getStock()));

        // 5. TTL 설정 (쿠폰 유효 기간 + 1일)
        long ttl = savedCoupon.getValidUntil() - System.currentTimeMillis() + 86400000L;
        redisTemplate.expire(stockKey, ttl, TimeUnit.MILLISECONDS);

        log.info("쿠폰 생성 완료 - couponId: {}, name: {}, stock: {}, Redis 재고 초기화 완료",
            savedCoupon.getId(), savedCoupon.getCouponName(), savedCoupon.getStock());

        return savedCoupon;
    }

    private void validateRequest(CreateCouponRequest request) {
        // 쿠폰명 검증
        if (request.couponName() == null || request.couponName().trim().isEmpty()) {
            throw new IllegalArgumentException("쿠폰명은 필수입니다.");
        }

        // 할인 타입 검증
        try {
            DiscountType.valueOf(request.discountType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("할인 타입은 PERCENT 또는 FIXED만 가능합니다.");
        }

        // 할인 금액 검증
        if (request.discountAmount() == null || request.discountAmount() <= 0) {
            throw new IllegalArgumentException("할인 금액은 0보다 커야 합니다.");
        }

        // PERCENT 타입이면 100 이하
        if ("PERCENT".equals(request.discountType()) && request.discountAmount() > 100) {
            throw new IllegalArgumentException("할인 비율은 100% 이하여야 합니다.");
        }

        // 최소 사용 금액 검증
        if (request.useMinAmount() == null || request.useMinAmount() < 0) {
            throw new IllegalArgumentException("최소 사용 금액은 0 이상이어야 합니다.");
        }

        // 최대 할인 금액 검증
        if (request.useMaxAmount() == null || request.useMaxAmount() <= 0) {
            throw new IllegalArgumentException("최대 할인 금액은 0보다 커야 합니다.");
        }

        // 재고 검증
        if (request.stock() == null || request.stock() <= 0) {
            throw new IllegalArgumentException("재고는 0보다 커야 합니다.");
        }

        // 유효 기간 검증
        if (request.validFrom() == null || request.validUntil() == null) {
            throw new IllegalArgumentException("유효 기간은 필수입니다.");
        }

        if (request.validFrom() >= request.validUntil()) {
            throw new IllegalArgumentException("종료 시간은 시작 시간보다 이후여야 합니다.");
        }

        // long now = System.currentTimeMillis();
        // if (request.validFrom() < now) {
        //     throw new IllegalArgumentException("시작 시간은 현재 시간 이후여야 합니다.");
        // }
    }
}