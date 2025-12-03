package com.hhplus.ecommerce.application.order;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.domain.order.event.OrderCancelledEvent;
import com.hhplus.ecommerce.infrastructure.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CancelOrderUseCase {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderEntity execute(long orderId) {
        OrderEntity order = orderRepository.getOrThrow(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 주문입니다.");
        }

        long now = System.currentTimeMillis();
        OrderEntity cancelledOrder = new OrderEntity(
            order.getId(),
            order.getUserId(),
            order.getTotalAmount(),
            order.getDiscountAmount(),
            order.getFinalAmount(),
            order.getCouponHistoryId(),
            OrderStatus.CANCELLED,
            order.getOrderedAt(),
            order.getCreatedAt(),
            now
        );
        OrderEntity savedOrder = orderRepository.save(cancelledOrder);

        // 주문 아이템 조회
        List<OrderItemEntity> orderItems = orderRepository.findItemsByOrderId(orderId);

        // 이벤트 발행 (트랜잭션 커밋 후 비동기 실행)
        eventPublisher.publishEvent(new OrderCancelledEvent(
            savedOrder.getId(),
            orderItems,
            savedOrder.getUpdatedAt()
        ));

        return savedOrder;
    }
}
