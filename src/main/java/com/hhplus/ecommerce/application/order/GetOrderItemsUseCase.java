package com.hhplus.ecommerce.application.order;

import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import com.hhplus.ecommerce.infrastructure.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetOrderItemsUseCase {

    private final OrderRepository orderRepository;

    public List<OrderItemEntity> execute(Long orderId) {
        return orderRepository.findItemsByOrderId(orderId);
    }
}