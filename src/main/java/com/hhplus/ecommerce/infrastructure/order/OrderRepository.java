package com.hhplus.ecommerce.infrastructure.order;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;

public interface OrderRepository {
    OrderEntity save(OrderEntity order);
    OrderItemEntity saveItem(OrderItemEntity orderItem);
}
