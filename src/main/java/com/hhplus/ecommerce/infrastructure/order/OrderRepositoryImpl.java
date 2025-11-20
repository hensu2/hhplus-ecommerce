package com.hhplus.ecommerce.infrastructure.order;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import com.hhplus.ecommerce.infrastructure.order.jpa.OrderItemJpaRepository;
import com.hhplus.ecommerce.infrastructure.order.jpa.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;
    private final OrderItemJpaRepository orderItemJpaRepository;

    @Override
    public OrderEntity save(OrderEntity order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public OrderItemEntity saveItem(OrderItemEntity orderItem) {
        return orderItemJpaRepository.save(orderItem);
    }

    @Override
    public Optional<OrderEntity> findById(long orderId) {
        return orderJpaRepository.findById(orderId);
    }

    @Override
    public List<OrderItemEntity> findItemsByOrderId(Long orderId) {
        return orderItemJpaRepository.findByOrderId(orderId);
    }
}
