package com.hhplus.ecommerce.infrastructure.order;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import com.hhplus.ecommerce.infrastructure.order.memory.OrderItemTable;
import com.hhplus.ecommerce.infrastructure.order.memory.OrderTable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderTable orderTable;
    private final OrderItemTable orderItemTable;

    public OrderRepositoryImpl(OrderTable orderTable, OrderItemTable orderItemTable) {
        this.orderTable = orderTable;
        this.orderItemTable = orderItemTable;
    }

    @Override
    public OrderEntity save(OrderEntity order) {
        return orderTable.save(order);
    }

    @Override
    public OrderItemEntity saveItem(OrderItemEntity orderItem) {
        return orderItemTable.save(orderItem);
    }

    @Override
    public Optional<OrderEntity> findById(long orderId) {
        return orderTable.findById(orderId);
    }
}
