package com.hhplus.ecommerce.application.order;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final SalesRankingService salesRankingService;
    private final FailedEventRepository failedEventRepository;
    private final ObjectMapper objectMapper;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
        retryFor = Exception.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000)
    )
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("주문 생성 이벤트 수신 - orderId: {} (재시도 처리 중)", event.getOrderId());

        for (OrderItemEntity item : event.getOrderItems()) {
            salesRankingService.increaseRanking(
                item.getProductId(),
                item.getQuantity(),
                event.getOrderedAt()
            );
        }

        log.info("판매 랭킹 업데이트 완료 - orderId: {}", event.getOrderId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
        retryFor = Exception.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000)
    )
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("주문 취소 이벤트 수신 - orderId: {} (재시도 처리 중)", event.getOrderId());

        for (OrderItemEntity item : event.getOrderItems()) {
            salesRankingService.decreaseRanking(
                item.getProductId(),
                item.getQuantity(),
                event.getCancelledAt()
            );
        }

        log.info("판매 랭킹 차감 완료 - orderId: {}", event.getOrderId());
    }

    @Recover
    public void recoverOrderCreated(Exception e, OrderCreatedEvent event) {
        log.error("주문 생성 이벤트 처리 최종 실패 - orderId: {}, 실패 이벤트 DB 저장", event.getOrderId(), e);

        try {
            String eventPayload = objectMapper.writeValueAsString(event);
            FailedEventEntity failedEvent = new FailedEventEntity(
                EventType.ORDER_CREATED,
                event.getOrderId(),
                eventPayload,
                e.getMessage()
            );
            failedEventRepository.save(failedEvent);
            log.info("실패 이벤트 저장 완료 - orderId: {}", event.getOrderId());
        } catch (JsonProcessingException jsonException) {
            log.error("실패 이벤트 JSON 변환 실패 - orderId: {}", event.getOrderId(), jsonException);
        } catch (Exception saveException) {
            log.error("실패 이벤트 저장 실패 - orderId: {}", event.getOrderId(), saveException);
        }
    }

    @Recover
    public void recoverOrderCancelled(Exception e, OrderCancelledEvent event) {
        log.error("주문 취소 이벤트 처리 최종 실패 - orderId: {}, 실패 이벤트 DB 저장", event.getOrderId(), e);

        try {
            String eventPayload = objectMapper.writeValueAsString(event);
            FailedEventEntity failedEvent = new FailedEventEntity(
                EventType.ORDER_CANCELLED,
                event.getOrderId(),
                eventPayload,
                e.getMessage()
            );
            failedEventRepository.save(failedEvent);
            log.info("실패 이벤트 저장 완료 - orderId: {}", event.getOrderId());
        } catch (JsonProcessingException jsonException) {
            log.error("실패 이벤트 JSON 변환 실패 - orderId: {}", event.getOrderId(), jsonException);
        } catch (Exception saveException) {
            log.error("실패 이벤트 저장 실패 - orderId: {}", event.getOrderId(), saveException);
        }
    }
}