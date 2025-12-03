package com.hhplus.ecommerce.application.order;

import com.hhplus.ecommerce.application.salesRanking.SalesRankingService;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import com.hhplus.ecommerce.domain.order.event.OrderCancelledEvent;
import com.hhplus.ecommerce.domain.order.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final SalesRankingService salesRankingService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        try {
            log.info("주문 생성 이벤트 수신 - orderId: {}", event.getOrderId());

            for (OrderItemEntity item : event.getOrderItems()) {
                salesRankingService.increaseRanking(
                    item.getProductId(),
                    item.getQuantity(),
                    event.getOrderedAt()
                );
            }

            log.info("판매 랭킹 업데이트 완료 - orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("판매 랭킹 업데이트 실패 - orderId: {}", event.getOrderId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCancelled(OrderCancelledEvent event) {
        try {
            log.info("주문 취소 이벤트 수신 - orderId: {}", event.getOrderId());

            for (OrderItemEntity item : event.getOrderItems()) {
                salesRankingService.decreaseRanking(
                    item.getProductId(),
                    item.getQuantity(),
                    event.getCancelledAt()
                );
            }

            log.info("판매 랭킹 차감 완료 - orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("판매 랭킹 차감 실패 - orderId: {}", event.getOrderId(), e);
        }
    }
}