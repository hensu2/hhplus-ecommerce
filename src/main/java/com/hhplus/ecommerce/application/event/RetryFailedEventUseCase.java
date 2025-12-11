package com.hhplus.ecommerce.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.application.salesRanking.SalesRankingService;
import com.hhplus.ecommerce.domain.event.EventType;
import com.hhplus.ecommerce.domain.event.FailedEventEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import com.hhplus.ecommerce.domain.order.event.OrderCancelledEvent;
import com.hhplus.ecommerce.domain.order.event.OrderCreatedEvent;
import com.hhplus.ecommerce.infrastructure.event.FailedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetryFailedEventUseCase {

    private final FailedEventRepository failedEventRepository;
    private final SalesRankingService salesRankingService;
    private final ObjectMapper objectMapper;

    @Transactional
    public FailedEventEntity execute(Long eventId) {
        FailedEventEntity failedEvent = failedEventRepository.getOrThrow(eventId);

        // 이미 성공한 이벤트는 재처리하지 않음
        if (failedEvent.getStatus() == com.hhplus.ecommerce.domain.event.FailedEventStatus.SUCCESS) {
            throw new IllegalStateException("이미 성공한 이벤트입니다. ID: " + eventId);
        }

        // 처리 중으로 변경
        failedEvent.markAsProcessing();
        failedEventRepository.save(failedEvent);

        try {
            log.info("실패 이벤트 재처리 시작 - eventId: {}, type: {}", eventId, failedEvent.getEventType());

            // 이벤트 타입별로 재처리
            if (failedEvent.getEventType() == EventType.ORDER_CREATED) {
                retryOrderCreated(failedEvent);
            } else if (failedEvent.getEventType() == EventType.ORDER_CANCELLED) {
                retryOrderCancelled(failedEvent);
            }

            // 재시도 횟수 증가 및 성공으로 변경
            failedEvent.increaseRetryCount();
            failedEvent.markAsSuccess();
            FailedEventEntity savedEvent = failedEventRepository.save(failedEvent);

            log.info("실패 이벤트 재처리 성공 - eventId: {}", eventId);
            return savedEvent;

        } catch (Exception e) {
            log.error("실패 이벤트 재처리 실패 - eventId: {}", eventId, e);

            // 재시도 횟수 증가 및 실패로 변경
            failedEvent.increaseRetryCount();
            failedEvent.markAsFailed(e.getMessage());
            failedEventRepository.save(failedEvent);

            throw new RuntimeException("실패 이벤트 재처리에 실패했습니다. ID: " + eventId, e);
        }
    }

    private void retryOrderCreated(FailedEventEntity failedEvent) throws Exception {
        OrderCreatedEvent event = objectMapper.readValue(
            failedEvent.getEventPayload(),
            OrderCreatedEvent.class
        );

        for (OrderItemEntity item : event.getOrderItems()) {
            salesRankingService.increaseRanking(
                item.getProductId(),
                item.getQuantity(),
                event.getOrderedAt()
            );
        }
    }

    private void retryOrderCancelled(FailedEventEntity failedEvent) throws Exception {
        OrderCancelledEvent event = objectMapper.readValue(
            failedEvent.getEventPayload(),
            OrderCancelledEvent.class
        );

        for (OrderItemEntity item : event.getOrderItems()) {
            salesRankingService.decreaseRanking(
                item.getProductId(),
                item.getQuantity(),
                event.getCancelledAt()
            );
        }
    }
}