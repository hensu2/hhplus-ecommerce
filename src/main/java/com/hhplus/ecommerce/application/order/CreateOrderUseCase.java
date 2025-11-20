package com.hhplus.ecommerce.application.order;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.infrastructure.order.OrderRepository;
import com.hhplus.ecommerce.infrastructure.product.ProductRepository;
import com.hhplus.ecommerce.infrastructure.productOption.ProductOptionRepository;
import com.hhplus.ecommerce.presentation.order.req.CreateOrderRequest;
import com.hhplus.ecommerce.presentation.order.req.OrderItemRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductRepository productRepository;

    public OrderEntity execute(CreateOrderRequest request) {
        long now = System.currentTimeMillis();

        List<OrderItemEntity> orderItems = new ArrayList<>();
        int totalAmount = 0;

        for (OrderItemRequest item : request.items()) {
            ProductOptionEntity option = productOptionRepository.getOrThrow(item.productOptionId());
            ProductEntity product = productRepository.getOrThrow(option.getProductId());

            productOptionRepository.decreaseStock(item.productOptionId(), item.quantity());

            // 상품 기본 가격 + 옵션 추가 가격
            int itemPrice = (product.getPrice().intValue() + option.getAdditionalPrice().intValue()) * item.quantity();
            totalAmount += itemPrice;

            orderItems.add(new OrderItemEntity(
                0L,
                0L,
                option.getProductId(),
                option.getId(),
                product.getProductName(),
                option.getOptionType(),
                item.quantity(),
                itemPrice,
                now
            ));
        }

        int discountAmount = 0;
        int finalAmount = totalAmount - discountAmount;

        OrderEntity order = new OrderEntity(
            0L,
            request.userId(),
            totalAmount,
            discountAmount,
            finalAmount,
            request.couponHistoryId(),
            OrderStatus.PENDING,
            now,
            now,
            now
        );
        OrderEntity savedOrder = orderRepository.save(order);

        for (OrderItemEntity orderItem : orderItems) {
            OrderItemEntity itemWithOrderId = new OrderItemEntity(
                orderItem.getId(),
                savedOrder.getId(),
                orderItem.getProductId(),
                orderItem.getProductOptionId(),
                orderItem.getProductName(),
                orderItem.getOptionType(),
                orderItem.getQuantity(),
                orderItem.getPrice(),
                orderItem.getCreatedAt()
            );
            orderRepository.saveItem(itemWithOrderId);
        }

        return savedOrder;
    }
}
