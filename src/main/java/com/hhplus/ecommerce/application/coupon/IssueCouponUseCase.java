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
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueCouponUseCase {

    private final CouponRepository couponRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // Lua 스크립트: 중복 체크 + 재고 차감을 원자적으로 수행
    private static final String ISSUE_COUPON_SCRIPT =
        "local issuedKey = KEYS[1]\n" +
        "local stockKey = KEYS[2]\n" +
        "local userId = ARGV[1]\n" +
        "local ttl = ARGV[2]\n" +
        "\n" +
        "-- 1. 중복 체크 (SADD는 이미 존재하면 0 반환)\n" +
        "local addResult = redis.call('SADD', issuedKey, userId)\n" +
        "if addResult == 0 then\n" +
        "    return -1  -- 이미 발급받음\n" +
        "end\n" +
        "\n" +
        "-- 2. TTL 설정\n" +
        "redis.call('PEXPIRE', issuedKey, ttl)\n" +
        "\n" +
        "-- 3. 재고 차감\n" +
        "local stock = redis.call('DECR', stockKey)\n" +
        "if stock < 0 then\n" +
        "    -- 재고 부족: 복구 및 Set에서 제거\n" +
        "    redis.call('INCR', stockKey)\n" +
        "    redis.call('SREM', issuedKey, userId)\n" +
        "    return -2  -- 재고 소진\n" +
        "end\n" +
        "\n" +
        "return stock  -- 남은 재고 반환\n";

    public IssueCouponResponse execute(long userId, long couponId) {
        // 1. 쿠폰 유효성 검증 (DB - 캐시 활용)
        CouponEntity coupon = couponRepository.getOrThrow(couponId);
        long now = System.currentTimeMillis();

        if (now < coupon.getValidFrom() || now > coupon.getValidUntil()) {
            throw new IllegalStateException("쿠폰 유효 기간이 아닙니다.");
        }

        // 2. Lua 스크립트로 중복 체크 + 재고 차감을 원자적으로 수행
        String issuedSetKey = "coupon:issued:" + couponId;
        String stockKey = "coupon:stock:" + couponId;
        long ttl = coupon.getValidUntil() - now + 604800000L; // 쿠폰 종료 + 7일

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(ISSUE_COUPON_SCRIPT);
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(
            script,
            Arrays.asList(issuedSetKey, stockKey),
            String.valueOf(userId),
            String.valueOf(ttl)
        );

        if (result == null) {
            throw new RuntimeException("쿠폰 발급 처리 중 오류가 발생했습니다.");
        }

        if (result == -1) {
            throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
        }

        if (result == -2) {
            throw new IllegalStateException("쿠폰이 모두 소진되었습니다.");
        }

        Long remainingStock = result;

        // 3. 발급 요청 큐에 추가 (Redis List)
        try {
            String queueKey = "coupon:issue:queue:" + couponId;
            CouponIssueRequest request = new CouponIssueRequest(
                userId,
                couponId,
                System.currentTimeMillis()
            );

            String requestJson = objectMapper.writeValueAsString(request);
            redisTemplate.opsForList().leftPush(queueKey, requestJson);

            log.info("쿠폰 발급 요청 접수 - userId: {}, couponId: {}, 남은 재고: {}",
                userId, couponId, remainingStock);

            // 4. 즉시 응답 반환
            return new IssueCouponResponse(
                "PENDING",
                "쿠폰 발급 요청이 접수되었습니다.",
                couponId,
                userId,
                System.currentTimeMillis()
            );

        } catch (JsonProcessingException e) {
            // JSON 변환 실패 시 재고 복구 및 Set에서 제거
            redisTemplate.opsForValue().increment(stockKey);
            redisTemplate.opsForSet().remove(issuedSetKey, String.valueOf(userId));
            throw new RuntimeException("쿠폰 발급 요청 처리 중 오류가 발생했습니다.", e);
        }
    }
}
