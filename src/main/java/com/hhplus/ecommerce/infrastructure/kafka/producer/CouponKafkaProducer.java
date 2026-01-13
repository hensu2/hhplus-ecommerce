package com.hhplus.ecommerce.infrastructure.kafka.producer;

import com.hhplus.ecommerce.domain.coupon.event.kafka.CouponKafkaEvent;
import com.hhplus.ecommerce.infrastructure.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 쿠폰 Kafka 프로듀서
 * 쿠폰 관련 이벤트를 Kafka에 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 쿠폰 이벤트 발행
     *
     * @param event 쿠폰 이벤트
     */
    public void publish(CouponKafkaEvent event) {
        try {
            // 파티션 키: userId (동일 사용자는 동일 파티션으로 순서 보장)
            String key = String.valueOf(getUserIdFromEvent(event));

            CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaTopics.COUPON_EVENTS, key, event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("쿠폰 이벤트 발행 성공 - eventType: {}, key: {}, partition: {}",
                        event.getEventType(),
                        key,
                        result.getRecordMetadata().partition());
                } else {
                    log.error("쿠폰 이벤트 발행 실패 - eventType: {}, key: {}",
                        event.getEventType(), key, ex);
                }
            });

        } catch (Exception e) {
            log.error("쿠폰 이벤트 발행 중 예외 발생 - eventType: {}",
                event.getEventType(), e);
            throw new RuntimeException("쿠폰 이벤트 발행 실패", e);
        }
    }

    /**
     * 이벤트에서 userId 추출 (파티션 키로 사용)
     */
    private Long getUserIdFromEvent(CouponKafkaEvent event) {
        // 리플렉션을 사용하여 userId 필드 접근
        try {
            var userIdField = event.getClass().getDeclaredField("userId");
            userIdField.setAccessible(true);
            return (Long) userIdField.get(event);
        } catch (Exception e) {
            log.warn("userId 추출 실패, 기본값 0 사용", e);
            return 0L;
        }
    }
}