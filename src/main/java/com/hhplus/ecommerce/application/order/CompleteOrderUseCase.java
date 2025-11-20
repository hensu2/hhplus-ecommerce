package com.hhplus.ecommerce.application.order;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.infrastructure.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompleteOrderUseCase {

    private final OrderRepository orderRepository;

    public OrderEntity execute(long orderId) {
        OrderEntity order = orderRepository.getOrThrow(orderId);
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("이미 완료된 주문입니다.");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("취소된 주문은 완료할 수 없습니다.");
        }

        long now = System.currentTimeMillis();
        OrderEntity completedOrder = new OrderEntity(
            order.getId(),
            order.getUserId(),
            order.getTotalAmount(),
            order.getDiscountAmount(),
            order.getFinalAmount(),
            order.getCouponHistoryId(),
            OrderStatus.COMPLETED,
            order.getOrderedAt(),
            order.getCreatedAt(),
            now
        );
        return orderRepository.save(completedOrder);
    }
}
