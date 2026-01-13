package com.hhplus.ecommerce.infrastructure.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.domain.event.EventType;
import com.hhplus.ecommerce.domain.event.FailedEventEntity;
import com.hhplus.ecommerce.domain.productOption.event.kafka.StockChangedKafkaEvent;
import com.hhplus.ecommerce.domain.productOption.event.kafka.StockKafkaEvent;
import com.hhplus.ecommerce.domain.stockHistory.StockHistoryEntity;
import com.hhplus.ecommerce.infrastructure.event.FailedEventRepository;
import com.hhplus.ecommerce.infrastructure.kafka.KafkaTopics;
import com.hhplus.ecommerce.infrastructure.stockHistory.StockHistoryRepository;
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
 * 재고 이벤트 Kafka Consumer
 * stock-events 토픽을 구독하여 재고 변경 이력을 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventKafkaConsumer {

    private final StockHistoryRepository stockHistoryRepository;
    private final FailedEventRepository failedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = KafkaTopics.STOCK_EVENTS,
        groupId = "stock-history-consumer-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 2000, multiplier = 2.0),
        dltTopicSuffix = "-dlq",
        include = {Exception.class}
    )
    public void consume(
        @Payload StockKafkaEvent event,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        Acknowledgment ack
    ) {
        try {
            log.info("재고 이벤트 수신 - eventType: {}, productOptionId: {}, key: {}",
                event.getEventType(), event.getProductOptionId(), key);

            handleStockChanged((StockChangedKafkaEvent) event);

            // 수동 커밋
            ack.acknowledge();
            log.info("재고 이벤트 처리 완료 - productOptionId: {}", event.getProductOptionId());

        } catch (Exception e) {
            log.error("재고 이벤트 처리 실패 - productOptionId: {}", event.getProductOptionId(), e);
            throw e; // @RetryableTopic이 재시도 처리
        }
    }

    /**
     * 재고 변경 이벤트 처리
     * 재고 변경 이력을 DB에 저장
     */
    private void handleStockChanged(StockChangedKafkaEvent event) {
        StockHistoryEntity history = StockHistoryEntity.from(
            event.getProductOptionId(),
            event.getProductId(),
            event.getProductName(),
            event.getOptionType(),
            event.getPreviousStock(),
            event.getCurrentStock(),
            event.getChangeAmount(),
            event.getChangeReason(),
            event.getEventType().name(),
            event.getTimestamp()
        );

        stockHistoryRepository.save(history);
        log.info("재고 변경 이력 저장 완료 - productOptionId: {}, reason: {}",
            event.getProductOptionId(), event.getChangeReason());
    }

    /**
     * Dead Letter Queue (DLQ) 핸들러
     * 재시도 실패한 메시지를 DB에 저장
     */
    @DltHandler
    public void handleDlt(
        @Payload StockKafkaEvent event,
        @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage
    ) {
        log.error("재고 이벤트 DLQ 도착 - productOptionId: {}, 실패 이벤트 DB 저장",
            event.getProductOptionId());

        try {
            String eventPayload = objectMapper.writeValueAsString(event);
            EventType eventType = switch (event.getEventType()) {
                case STOCK_INCREASED -> EventType.STOCK_INCREASED;
                case STOCK_DECREASED -> EventType.STOCK_DECREASED;
                case STOCK_UPDATED -> EventType.STOCK_UPDATED;
            };

            FailedEventEntity failedEvent = new FailedEventEntity(
                eventType,
                event.getProductOptionId(),
                eventPayload,
                exceptionMessage
            );
            failedEventRepository.save(failedEvent);
            log.info("실패 이벤트 저장 완료 - productOptionId: {}, 수동 재처리 가능", event.getProductOptionId());

        } catch (Exception e) {
            log.error("실패 이벤트 저장 실패 - productOptionId: {}", event.getProductOptionId(), e);
        }
    }
}
