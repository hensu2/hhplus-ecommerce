package com.hhplus.ecommerce.infrastructure.kafka.consumer;

import com.hhplus.ecommerce.domain.productOption.event.kafka.StockChangedKafkaEvent;
import com.hhplus.ecommerce.domain.productOption.event.kafka.StockEventType;
import com.hhplus.ecommerce.domain.productOption.event.kafka.StockKafkaEvent;
import com.hhplus.ecommerce.domain.stockAnalytics.StockAnalyticsEntity;
import com.hhplus.ecommerce.infrastructure.kafka.KafkaTopics;
import com.hhplus.ecommerce.infrastructure.stockAnalytics.StockAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

/**
 * 재고 분석 Consumer
 * 일별 재고 변동 통계를 집계
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockAnalyticsConsumer {

    private final StockAnalyticsRepository stockAnalyticsRepository;

    @KafkaListener(
        topics = KafkaTopics.STOCK_EVENTS,
        groupId = "stock-analytics-consumer-group",
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
            log.info("재고 분석 Consumer - eventType: {}, productOptionId: {}",
                event.getEventType(), event.getProductOptionId());

            handleStockChanged((StockChangedKafkaEvent) event);

            ack.acknowledge();
            log.info("재고 분석 처리 완료 - productOptionId: {}", event.getProductOptionId());

        } catch (Exception e) {
            log.error("재고 분석 처리 실패 - productOptionId: {}", event.getProductOptionId(), e);
            throw e;
        }
    }

    private void handleStockChanged(StockChangedKafkaEvent event) {
        // 이벤트 발생 날짜 계산 (YYYY-MM-DD 형식)
        LocalDate eventDate = Instant.ofEpochMilli(event.getTimestamp())
            .atZone(ZoneId.systemDefault())
            .toLocalDate();
        String dateStr = eventDate.toString();

        // 해당 날짜의 통계 조회 또는 생성
        StockAnalyticsEntity analytics = stockAnalyticsRepository
            .findByProductOptionIdAndDate(event.getProductOptionId(), dateStr)
            .orElseGet(() -> StockAnalyticsEntity.create(
                event.getProductOptionId(),
                event.getProductId(),
                event.getProductName(),
                event.getOptionType(),
                dateStr
            ));

        // 이벤트 타입에 따라 통계 업데이트
        StockAnalyticsEntity updatedAnalytics;
        if (event.getEventType() == StockEventType.STOCK_INCREASED) {
            updatedAnalytics = analytics.addIncrease(
                event.getChangeAmount(),
                event.getChangeReason()
            );
        } else {
            updatedAnalytics = analytics.addDecrease(
                event.getChangeAmount(),
                event.getChangeReason()
            );
        }

        stockAnalyticsRepository.save(updatedAnalytics);
        log.info("재고 분석 통계 업데이트 - productOptionId: {}, date: {}, eventType: {}",
            event.getProductOptionId(), dateStr, event.getEventType());
    }
}
