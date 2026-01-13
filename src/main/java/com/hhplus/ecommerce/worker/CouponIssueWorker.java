package com.hhplus.ecommerce.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.application.coupon.dto.CouponIssuePending;
import com.hhplus.ecommerce.application.coupon.dto.CouponIssueRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponIssueWorker {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 100ms마다 큐에서 요청 가져와서 처리
     */
    @Scheduled(fixedDelay = 100)
    public void processIssueQueue() {
        // 모든 쿠폰 큐를 스캔
        Set<String> queueKeys = redisTemplate.keys("coupon:issue:queue:*");

        if (queueKeys == null || queueKeys.isEmpty()) {
            return;
        }

        for (String queueKey : queueKeys) {
            // 재시도 큐는 스킵
            if (queueKey.contains(":retry")) {
                continue;
            }

            processCouponQueue(queueKey);
        }
    }

    private void processCouponQueue(String queueKey) {
        // 큐에서 최대 10개씩 배치 처리
        int batchSize = 10;

        for (int i = 0; i < batchSize; i++) {
            String requestJson = redisTemplate.opsForList().rightPop(queueKey);

            if (requestJson == null) {
                break; // 큐가 비었음
            }

            try {
                CouponIssueRequest request = objectMapper.readValue(
                    requestJson,
                    CouponIssueRequest.class
                );

                processIssueRequest(request);

            } catch (JsonProcessingException e) {
                log.error("JSON 파싱 실패 - request: {}", requestJson, e);
                // 파싱 실패 시 재시도 큐로 이동
                moveToRetryQueue(queueKey, requestJson);
            } catch (Exception e) {
                log.error("쿠폰 발급 처리 실패 - request: {}", requestJson, e);
                // 처리 실패 시 재시도 큐로 이동
                moveToRetryQueue(queueKey, requestJson);
            }
        }
    }

    private void processIssueRequest(CouponIssueRequest request) {
        Long userId = request.getUserId();
        Long couponId = request.getCouponId();
        Long requestedAt = request.getRequestedAt();

        // Redis Hash에 임시 저장 (DB 동기화 전)
        String pendingKey = "coupon:issued:pending:" + couponId;

        CouponIssuePending pending = new CouponIssuePending(
            0L,  // historyId는 DB 저장 후 생성
            userId,
            couponId,
            "ISSUED",
            requestedAt
        );

        try {
            String pendingJson = objectMapper.writeValueAsString(pending);
            redisTemplate.opsForHash().put(
                pendingKey,
                String.valueOf(userId),
                pendingJson
            );

            // TTL 설정 (24시간)
            redisTemplate.expire(pendingKey, 24, TimeUnit.HOURS);

            log.info("쿠폰 발급 임시 저장 완료 - userId: {}, couponId: {}", userId, couponId);

        } catch (JsonProcessingException e) {
            log.error("Redis Hash 저장 실패", e);
            throw new RuntimeException("쿠폰 발급 처리 실패", e);
        }
    }

    private void moveToRetryQueue(String originalQueueKey, String requestJson) {
        String retryQueueKey = originalQueueKey + ":retry";
        redisTemplate.opsForList().leftPush(retryQueueKey, requestJson);
        log.warn("재시도 큐로 이동 - queue: {}", retryQueueKey);
    }
}