package com.hhplus.ecommerce.infrastructure.order.memory;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class OrderTable {
    private final ConcurrentHashMap<Long, OrderEntity> table = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public OrderEntity save(OrderEntity order) {
        long id = order.id() == 0 ? idGenerator.getAndIncrement() : order.id();
        OrderEntity newOrder = new OrderEntity(
            id,
            order.userId(),
            order.totalAmount(),
            order.discountAmount(),
            order.finalAmount(),
            order.couponHistoryId(),
            order.status(),
            order.createdAt(),
            order.updatedAt()
        );
        table.put(id, newOrder);
        return newOrder;
    }
}
