package com.hhplus.ecommerce.infrastructure.order.memory;

import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class OrderItemTable {
    private final ConcurrentHashMap<Long, OrderItemEntity> table = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public OrderItemEntity save(OrderItemEntity orderItem) {
        long id = orderItem.id() == 0 ? idGenerator.getAndIncrement() : orderItem.id();
        OrderItemEntity newOrderItem = new OrderItemEntity(
            id,
            orderItem.orderId(),
            orderItem.productId(),
            orderItem.productOptionId(),
            orderItem.productName(),
            orderItem.optionType(),
            orderItem.quantity(),
            orderItem.price(),
            orderItem.createdAt()
        );
        table.put(id, newOrderItem);
        return newOrderItem;
    }
}
