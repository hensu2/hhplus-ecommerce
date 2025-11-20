package com.hhplus.ecommerce.infrastructure.order.memory;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class OrderTable {
    private final ConcurrentHashMap<Long, OrderEntity> table = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public OrderEntity save(OrderEntity order) {
        long id = order.getId() == null ? idGenerator.getAndIncrement() : order.getId();
        OrderEntity newOrder = new OrderEntity(
            id,
            order.getUserId(),
            order.getTotalAmount(),
            order.getDiscountAmount(),
            order.getFinalAmount(),
            order.getCouponHistoryId(),
            order.getStatus(),
            order.getOrderedAt(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
        table.put(id, newOrder);
        return newOrder;
    }

    public Optional<OrderEntity> findById(long orderId) {
        return Optional.ofNullable(table.get(orderId));
    }
}
