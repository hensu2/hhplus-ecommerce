package com.hhplus.ecommerce.infrastructure.order;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    OrderEntity save(OrderEntity order);
    OrderItemEntity saveItem(OrderItemEntity orderItem);
    Optional<OrderEntity> findById(long orderId);
    List<OrderItemEntity> findItemsByOrderId(Long orderId);

    default OrderEntity getOrThrow(long orderId) {
        return findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
    }
}
