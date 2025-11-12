package com.hhplus.ecommerce.application.order;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionService;
import com.hhplus.ecommerce.infrastructure.order.OrderRepository;
import com.hhplus.ecommerce.infrastructure.productOption.ProductOptionRepository;
import com.hhplus.ecommerce.presentation.order.req.CreateOrderRequest;
import com.hhplus.ecommerce.presentation.order.req.OrderItemRequest;
import com.hhplus.ecommerce.presentation.order.res.OrderItemResponse;
import com.hhplus.ecommerce.presentation.order.res.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductOptionService productOptionService;

    public OrderResponse execute(CreateOrderRequest request) {
        long now = System.currentTimeMillis();

        List<OrderItemEntity> orderItems = new ArrayList<>();
        int totalAmount = 0;

        for (OrderItemRequest item : request.items()) {
            ProductOptionEntity option = productOptionRepository.findById(item.productOptionId())
                .orElseThrow(() -> new IllegalArgumentException("상품 옵션을 찾을 수 없습니다."));

            ProductOptionEntity updatedOption = productOptionService.decreaseStock(
                item.productOptionId(),
                item.quantity()
            );

            int itemPrice = (int) (option.additionalPrice() * item.quantity());
            totalAmount += itemPrice;

            orderItems.add(new OrderItemEntity(
                0L,
                0L,
                option.productId(),
                option.id(),
                "상품명",
                option.optionType(),
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
            OrderStatus.COMPLETED,
            now,
            now
        );
        OrderEntity savedOrder = orderRepository.save(order);

        List<OrderItemResponse> itemResponses = new ArrayList<>();
        for (OrderItemEntity orderItem : orderItems) {
            OrderItemEntity itemWithOrderId = new OrderItemEntity(
                orderItem.id(),
                savedOrder.id(),
                orderItem.productId(),
                orderItem.productOptionId(),
                orderItem.productName(),
                orderItem.optionType(),
                orderItem.quantity(),
                orderItem.price(),
                orderItem.createdAt()
            );
            OrderItemEntity savedItem = orderRepository.saveItem(itemWithOrderId);

            itemResponses.add(new OrderItemResponse(
                savedItem.productId(),
                savedItem.productOptionId(),
                savedItem.productName(),
                savedItem.optionType(),
                savedItem.quantity(),
                savedItem.price()
            ));
        }

        return new OrderResponse(
            savedOrder.id(),
            savedOrder.userId(),
            savedOrder.totalAmount(),
            savedOrder.discountAmount(),
            savedOrder.finalAmount(),
            savedOrder.status().name(),
            itemResponses,
            formatTimestamp(savedOrder.createdAt())
        );
    }

    private String formatTimestamp(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .toString();
    }
}
