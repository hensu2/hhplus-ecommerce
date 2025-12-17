package com.hhplus.ecommerce.infrastructure.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.application.salesRanking.SalesRankingService;
import com.hhplus.ecommerce.domain.event.EventType;
import com.hhplus.ecommerce.domain.event.FailedEventEntity;
import com.hhplus.ecommerce.domain.order.event.kafka.OrderCancelledKafkaEvent;
import com.hhplus.ecommerce.domain.order.event.kafka.OrderCreatedKafkaEvent;
import com.hhplus.ecommerce.domain.order.event.kafka.OrderEventType;
import com.hhplus.ecommerce.domain.order.event.kafka.OrderItemDto;
import com.hhplus.ecommerce.domain.order.event.kafka.OrderKafkaEvent;
import com.hhplus.ecommerce.infrastructure.event.FailedEventRepository;
import com.hhplus.ecommerce.infrastructure.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * 주문 이벤트 Kafka Consumer
 * order-events 토픽을 구독하여 판매 랭킹을 업데이트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventKafkaConsumer {

    private final SalesRankingService salesRankingService;
    private final FailedEventRepository failedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = KafkaTopics.ORDER_EVENTS,
        groupId = "sales-ranking-consumer-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 2000, multiplier = 2.0),
        dltTopicSuffix = "-dlq",
        include = {Exception.class}
    )
    public void consume(
        @Payload OrderKafkaEvent event,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        Acknowledgment ack
    ) {
        try {
            log.info("주문 이벤트 수신 - eventType: {}, orderId: {}, key: {}",
                event.getEventType(), event.getOrderId(), key);

            switch (event.getEventType()) {
                case ORDER_CREATED -> handleOrderCreated((OrderCreatedKafkaEvent) event);
                case ORDER_CANCELLED -> handleOrderCancelled((OrderCancelledKafkaEvent) event);
                default -> log.warn("알 수 없는 이벤트 타입: {}", event.getEventType());
            }

            // 수동 커밋
            ack.acknowledge();
            log.info("주문 이벤트 처리 완료 - orderId: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("주문 이벤트 처리 실패 - orderId: {}", event.getOrderId(), e);
            throw e; // @RetryableTopic이 재시도 처리
        }
    }

    /**
     * 주문 생성 이벤트 처리
     * 판매 랭킹 증가
     */
    private void handleOrderCreated(OrderCreatedKafkaEvent event) {
        for (OrderItemDto item : event.getOrderItems()) {
            salesRankingService.increaseRanking(
                item.getProductId(),
                item.getQuantity(),
                event.getTimestamp()
            );
        }
        log.info("판매 랭킹 증가 완료 - orderId: {}, itemCount: {}",
            event.getOrderId(), event.getOrderItems().size());
    }

    /**
     * 주문 취소 이벤트 처리
     * 판매 랭킹 감소
     */
    private void handleOrderCancelled(OrderCancelledKafkaEvent event) {
        for (OrderItemDto item : event.getOrderItems()) {
            salesRankingService.decreaseRanking(
                item.getProductId(),
                item.getQuantity(),
                event.getCancelledAt()
            );
        }
        log.info("판매 랭킹 감소 완료 - orderId: {}, itemCount: {}",
            event.getOrderId(), event.getOrderItems().size());
    }

    /**
     * Dead Letter Queue (DLQ) 핸들러
     * 재시도 실패한 메시지를 DB에 저장
     */
    @DltHandler
    public void handleDlt(
        @Payload OrderKafkaEvent event,
        @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage
    ) {
        log.error("주문 이벤트 DLQ 도착 - orderId: {}, 실패 이벤트 DB 저장",
            event.getOrderId());

        try {
            String eventPayload = objectMapper.writeValueAsString(event);
            EventType eventType = event.getEventType() == OrderEventType.ORDER_CREATED
                ? EventType.ORDER_CREATED : EventType.ORDER_CANCELLED;

            FailedEventEntity failedEvent = new FailedEventEntity(
                eventType,
                event.getOrderId(),
                eventPayload,
                exceptionMessage
            );
            failedEventRepository.save(failedEvent);
            log.info("실패 이벤트 저장 완료 - orderId: {}, 수동 재처리 가능", event.getOrderId());

        } catch (Exception e) {
            log.error("실패 이벤트 저장 실패 - orderId: {}", event.getOrderId(), e);
        }
    }
}
