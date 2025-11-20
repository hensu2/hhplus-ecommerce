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
        long id = orderItem.getId() == null ? idGenerator.getAndIncrement() : orderItem.getId();
        OrderItemEntity newOrderItem = new OrderItemEntity(
            id,
            orderItem.getOrderId(),
            orderItem.getProductId(),
            orderItem.getProductOptionId(),
            orderItem.getProductName(),
            orderItem.getOptionType(),
            orderItem.getQuantity(),
            orderItem.getPrice(),
            orderItem.getCreatedAt()
        );
        table.put(id, newOrderItem);
        return newOrderItem;
    }
}
