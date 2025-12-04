package com.hhplus.ecommerce.application.coupon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.application.coupon.dto.CouponIssueRequest;
import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.infrastructure.coupon.CouponRepository;
import com.hhplus.ecommerce.presentation.coupon.res.IssueCouponResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueCouponUseCase {

    private final CouponRepository couponRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public IssueCouponResponse execute(long userId, long couponId) {
        // 1. 쿠폰 유효성 검증 (DB - 캐시 활용)
        CouponEntity coupon = couponRepository.getOrThrow(couponId);
        long now = System.currentTimeMillis();

        if (now < coupon.getValidFrom() || now > coupon.getValidUntil()) {
            throw new IllegalStateException("쿠폰 유효 기간이 아닙니다.");
        }

        // 2. 중복 발급 체크 (Redis Set)
        String issuedSetKey = "coupon:issued:" + couponId;
        Boolean isAlreadyIssued = redisTemplate.opsForSet()
            .isMember(issuedSetKey, String.valueOf(userId));

        if (Boolean.TRUE.equals(isAlreadyIssued)) {
            throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
        }

        // 3. 재고 확인 및 차감 (Redis String)
        String stockKey = "coupon:stock:" + couponId;
        Long remainingStock = redisTemplate.opsForValue().decrement(stockKey);

        if (remainingStock == null || remainingStock < 0) {
            // 재고 복구
            if (remainingStock != null) {
                redisTemplate.opsForValue().increment(stockKey);
            }
            throw new IllegalStateException("쿠폰이 모두 소진되었습니다.");
        }

        // 4. 발급 요청 큐에 추가 (Redis List)
        String queueKey = "coupon:issue:queue:" + couponId;
        CouponIssueRequest request = new CouponIssueRequest(
            userId,
            couponId,
            System.currentTimeMillis()
        );

        try {
            String requestJson = objectMapper.writeValueAsString(request);
            redisTemplate.opsForList().leftPush(queueKey, requestJson);
        } catch (JsonProcessingException e) {
            // JSON 변환 실패 시 재고 복구
            redisTemplate.opsForValue().increment(stockKey);
            throw new RuntimeException("쿠폰 발급 요청 처리 중 오류가 발생했습니다.", e);
        }

        // 5. 중복 방지 Set에 추가 (Redis Set)
        redisTemplate.opsForSet().add(issuedSetKey, String.valueOf(userId));

        // 6. TTL 설정 (중복 방지 Set)
        long ttl = coupon.getValidUntil() - now + 604800000L; // 쿠폰 종료 + 7일
        redisTemplate.expire(issuedSetKey, ttl, TimeUnit.MILLISECONDS);

        log.info("쿠폰 발급 요청 접수 - userId: {}, couponId: {}, 남은 재고: {}",
            userId, couponId, remainingStock);

        // 7. 즉시 응답 반환
        return new IssueCouponResponse(
            "PENDING",
            "쿠폰 발급 요청이 접수되었습니다.",
            couponId,
            userId,
            System.currentTimeMillis()
        );
    }
}
