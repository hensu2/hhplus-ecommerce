package com.hhplus.ecommerce.infrastructure.kafka.consumer;

import com.hhplus.ecommerce.domain.lowStockAlert.LowStockAlertEntity;
import com.hhplus.ecommerce.domain.productOption.event.kafka.StockChangedKafkaEvent;
import com.hhplus.ecommerce.domain.productOption.event.kafka.StockEventType;
import com.hhplus.ecommerce.domain.productOption.event.kafka.StockKafkaEvent;
import com.hhplus.ecommerce.infrastructure.kafka.KafkaTopics;
import com.hhplus.ecommerce.infrastructure.lowStockAlert.LowStockAlertRepository;
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

import java.util.Optional;

/**
 * 재고 부족 알림 Consumer
 * 재고가 임계값 이하로 떨어지면 알림을 생성하고, 재고가 다시 증가하면 알림을 해제
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LowStockAlertConsumer {

    private static final Long LOW_STOCK_THRESHOLD = 10L;

    private final LowStockAlertRepository lowStockAlertRepository;

    @KafkaListener(
        topics = KafkaTopics.STOCK_EVENTS,
        groupId = "low-stock-alert-consumer-group",
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
            log.info("재고 부족 알림 Consumer - eventType: {}, productOptionId: {}",
                event.getEventType(), event.getProductOptionId());

            handleStockChanged((StockChangedKafkaEvent) event);

            ack.acknowledge();
            log.info("재고 부족 알림 처리 완료 - productOptionId: {}", event.getProductOptionId());

        } catch (Exception e) {
            log.error("재고 부족 알림 처리 실패 - productOptionId: {}", event.getProductOptionId(), e);
            throw e;
        }
    }

    private void handleStockChanged(StockChangedKafkaEvent event) {
        Long currentStock = event.getCurrentStock();
        Long productOptionId = event.getProductOptionId();

        // 재고가 임계값 이하로 떨어진 경우
        if (currentStock <= LOW_STOCK_THRESHOLD) {
            // 이미 미해결 알림이 있는지 확인
            Optional<LowStockAlertEntity> existingAlert =
                lowStockAlertRepository.findLatestUnresolvedAlert(productOptionId);

            if (existingAlert.isEmpty()) {
                // 새로운 알림 생성
                LowStockAlertEntity alert = LowStockAlertEntity.create(
                    productOptionId,
                    event.getProductId(),
                    event.getProductName(),
                    event.getOptionType(),
                    currentStock,
                    LOW_STOCK_THRESHOLD
                );
                lowStockAlertRepository.save(alert);
                log.warn("⚠️ 재고 부족 알림 생성 - {}: 현재 재고 {}개 (임계값: {}개)",
                    event.getProductName(), currentStock, LOW_STOCK_THRESHOLD);
            }
        }
        // 재고가 임계값을 초과한 경우
        else if (event.getEventType() == StockEventType.STOCK_INCREASED) {
            // 미해결 알림이 있으면 해제
            Optional<LowStockAlertEntity> existingAlert =
                lowStockAlertRepository.findLatestUnresolvedAlert(productOptionId);

            existingAlert.ifPresent(alert -> {
                LowStockAlertEntity resolvedAlert = alert.resolve();
                lowStockAlertRepository.save(resolvedAlert);
                log.info("✅ 재고 부족 알림 해제 - {}: 현재 재고 {}개",
                    event.getProductName(), currentStock);
            });
        }
    }
}
