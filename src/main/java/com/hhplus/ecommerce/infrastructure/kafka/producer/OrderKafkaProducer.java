package com.hhplus.ecommerce.infrastructure.kafka.producer;

import com.hhplus.ecommerce.domain.order.event.kafka.OrderKafkaEvent;
import com.hhplus.ecommerce.infrastructure.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 주문 Kafka 프로듀서
 * 주문 관련 이벤트를 Kafka에 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 주문 이벤트 발행
     *
     * @param event 주문 이벤트
     */
    public void publish(OrderKafkaEvent event) {
        try {
            // 파티션 키: orderId (동일 주문은 동일 파티션으로 순서 보장)
            String key = String.valueOf(event.getOrderId());

            CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaTopics.ORDER_EVENTS, key, event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("주문 이벤트 발행 성공 - eventType: {}, orderId: {}, partition: {}",
                        event.getEventType(),
                        event.getOrderId(),
                        result.getRecordMetadata().partition());
                } else {
                    log.error("주문 이벤트 발행 실패 - eventType: {}, orderId: {}",
                        event.getEventType(), event.getOrderId(), ex);
                }
            });

        } catch (Exception e) {
            log.error("주문 이벤트 발행 중 예외 발생 - eventType: {}, orderId: {}",
                event.getEventType(), event.getOrderId(), e);
            throw new RuntimeException("주문 이벤트 발행 실패", e);
        }
    }
}
