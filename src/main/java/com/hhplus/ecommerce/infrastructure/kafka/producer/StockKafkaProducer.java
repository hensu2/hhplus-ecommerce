package com.hhplus.ecommerce.infrastructure.kafka.producer;

import com.hhplus.ecommerce.domain.productOption.event.kafka.StockKafkaEvent;
import com.hhplus.ecommerce.infrastructure.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 재고 Kafka 프로듀서
 * 재고 관련 이벤트를 Kafka에 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 재고 이벤트 발행
     *
     * @param event 재고 이벤트
     */
    public void publish(StockKafkaEvent event) {
        try {
            // 파티션 키: productOptionId (동일 상품 옵션은 동일 파티션으로 순서 보장)
            String key = String.valueOf(event.getProductOptionId());

            CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaTopics.STOCK_EVENTS, key, event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("재고 이벤트 발행 성공 - eventType: {}, productOptionId: {}, partition: {}",
                        event.getEventType(),
                        event.getProductOptionId(),
                        result.getRecordMetadata().partition());
                } else {
                    log.error("재고 이벤트 발행 실패 - eventType: {}, productOptionId: {}",
                        event.getEventType(), event.getProductOptionId(), ex);
                }
            });

        } catch (Exception e) {
            log.error("재고 이벤트 발행 중 예외 발생 - eventType: {}, productOptionId: {}",
                event.getEventType(), event.getProductOptionId(), e);
            throw new RuntimeException("재고 이벤트 발행 실패", e);
        }
    }
}
