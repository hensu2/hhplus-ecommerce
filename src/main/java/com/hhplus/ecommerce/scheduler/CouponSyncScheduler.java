package com.hhplus.ecommerce.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.application.coupon.dto.CouponIssuePending;
import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;
import com.hhplus.ecommerce.domain.coupon.CouponStatus;
import com.hhplus.ecommerce.domain.coupon.event.kafka.CouponIssuedKafkaEvent;
import com.hhplus.ecommerce.infrastructure.coupon.CouponRepository;
import com.hhplus.ecommerce.infrastructure.kafka.producer.CouponKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponSyncScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final CouponRepository couponRepository;
    private final ObjectMapper objectMapper;
    private final CouponKafkaProducer couponKafkaProducer;

    /**
     * 10초마다 Redis → DB 동기화
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void syncCouponIssuesToDB() {
        log.info("쿠폰 발급 내역 DB 동기화 시작");

        // 모든 pending Hash 스캔
        Set<String> pendingKeys = redisTemplate.keys("coupon:issued:pending:*");

        if (pendingKeys == null || pendingKeys.isEmpty()) {
            log.debug("동기화할 데이터 없음");
            return;
        }

        int totalSynced = 0;
        int totalFailed = 0;

        for (String pendingKey : pendingKeys) {
            try {
                int synced = syncPendingCoupon(pendingKey);
                totalSynced += synced;
            } catch (Exception e) {
                log.error("동기화 실패 - key: {}", pendingKey, e);
                totalFailed++;
            }
        }

        log.info("쿠폰 발급 내역 DB 동기화 완료 - 성공: {}, 실패: {}", totalSynced, totalFailed);
    }

    private int syncPendingCoupon(String pendingKey) {
        // Redis Hash에서 모든 데이터 가져오기
        Map<Object, Object> pendingMap = redisTemplate.opsForHash().entries(pendingKey);

        if (pendingMap.isEmpty()) {
            return 0;
        }

        // couponId 추출 (key 형식: coupon:issued:pending:{couponId})
        Long couponId = Long.parseLong(pendingKey.split(":")[3]);

        List<CouponHistoryEntity> historiesToSave = new ArrayList<>();
        List<String> userIdsToDelete = new ArrayList<>();

        for (Map.Entry<Object, Object> entry : pendingMap.entrySet()) {
            String userIdStr = (String) entry.getKey();
            String pendingJson = (String) entry.getValue();

            try {
                CouponIssuePending pending = objectMapper.readValue(
                    pendingJson,
                    CouponIssuePending.class
                );

                // 이미 동기화된 데이터인지 확인
                String syncKey = "coupon:synced:histories";
                String syncValue = userIdStr + ":" + couponId;
                Boolean isSynced = redisTemplate.opsForSet().isMember(syncKey, syncValue);

                if (Boolean.TRUE.equals(isSynced)) {
                    log.warn("이미 동기화된 데이터 - userId: {}, couponId: {}", userIdStr, couponId);
                    userIdsToDelete.add(userIdStr);
                    continue;
                }

                // DB에 이미 존재하는지 확인 (중복 방지)
                if (couponRepository.findHistoryByUserIdAndCouponId(
                    Long.parseLong(userIdStr), couponId).isPresent()) {
                    log.warn("DB에 이미 존재하는 데이터 (동기화 스킵) - userId: {}, couponId: {}",
                        userIdStr, couponId);
                    userIdsToDelete.add(userIdStr);
                    continue;
                }

                // DB에 저장할 엔티티 생성
                CouponHistoryEntity history = new CouponHistoryEntity(
                    0L,  // ID 자동 생성
                    Long.parseLong(userIdStr),
                    couponId,
                    CouponStatus.valueOf(pending.getStatus()),
                    pending.getIssuedAt(),
                    null
                );

                historiesToSave.add(history);

            } catch (JsonProcessingException e) {
                log.error("JSON 파싱 실패 - userId: {}, json: {}", userIdStr, pendingJson, e);
            } catch (Exception e) {
                log.error("쿠폰 히스토리 처리 실패 - userId: {}", userIdStr, e);
            }
        }

        if (historiesToSave.isEmpty()) {
            // 삭제만 처리
            for (String userId : userIdsToDelete) {
                redisTemplate.opsForHash().delete(pendingKey, userId);
            }
            return 0;
        }

        // 배치 삽입
        List<CouponHistoryEntity> savedHistories = couponRepository.saveAllHistories(historiesToSave);

        // 쿠폰 정보 조회 (Kafka 이벤트 발행에 필요)
        CouponEntity coupon = couponRepository.getOrThrow(couponId);

        // Kafka 이벤트 발행 (트랜잭션 커밋 후)
        for (CouponHistoryEntity history : savedHistories) {
            try {
                CouponIssuedKafkaEvent event = new CouponIssuedKafkaEvent(history, coupon);
                couponKafkaProducer.publish(event);
                log.info("쿠폰 발급 이벤트 발행 - couponHistoryId: {}, userId: {}, couponId: {}",
                    history.getId(), history.getUserId(), history.getCouponId());
            } catch (Exception e) {
                log.error("쿠폰 발급 이벤트 발행 실패 - couponHistoryId: {}", history.getId(), e);
                // 이벤트 발행 실패는 비즈니스 로직에 영향을 주지 않음
            }
        }

        // 동기화 완료 마킹
        String syncKey = "coupon:synced:histories";
        for (CouponHistoryEntity history : savedHistories) {
            String syncValue = history.getUserId() + ":" + history.getCouponId();
            redisTemplate.opsForSet().add(syncKey, syncValue);
            userIdsToDelete.add(String.valueOf(history.getUserId()));
        }

        // TTL 설정 (7일)
        redisTemplate.expire(syncKey, 7, TimeUnit.DAYS);

        // Redis Hash에서 동기화 완료된 데이터 삭제
        for (String userId : userIdsToDelete) {
            redisTemplate.opsForHash().delete(pendingKey, userId);
        }

        log.info("DB 동기화 완료 - couponId: {}, count: {}", couponId, savedHistories.size());

        return savedHistories.size();
    }

    /**
     * 매일 자정 1시에 동기화 완료 마커 정리
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void cleanupSyncMarkers() {
        String syncKey = "coupon:synced:histories";
        redisTemplate.delete(syncKey);
        log.info("동기화 마커 정리 완료");
    }
}