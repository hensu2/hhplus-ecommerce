package com.hhplus.ecommerce.application.order;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionService;
import com.hhplus.ecommerce.domain.order.OrderRepository;
import com.hhplus.ecommerce.domain.productOption.ProductOptionRepository;
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
        List<OrderItemEntity> orderItems = new ArrayList<>();
        int totalAmount = 0;

        for (OrderItemRequest item : request.items()) {
            ProductOptionEntity option = productOptionRepository.findById(item.productOptionId())
                .orElseThrow(() -> new IllegalArgumentException("상품 옵션을 찾을 수 없습니다."));

            ProductOptionEntity updatedOption = productOptionService.decreaseStock(
                item.productOptionId(),
                item.quantity()
            );

            int itemPrice = (int) (option.getAdditionalPrice() * item.quantity());
            totalAmount += itemPrice;

            orderItems.add(OrderItemEntity.create(
                0L,
                option.getProduct().getId(),
                option.getId(),
                "상품명",
                option.getOptionType(),
                item.quantity(),
                itemPrice
            ));
        }

        int discountAmount = 0;
        int finalAmount = totalAmount - discountAmount;

        OrderEntity order = OrderEntity.create(
            request.userId(),
            totalAmount,
            discountAmount,
            finalAmount,
            request.couponHistoryId()
        );
        order.complete();
        OrderEntity savedOrder = orderRepository.save(order);

        List<OrderItemResponse> itemResponses = new ArrayList<>();
        for (OrderItemEntity orderItem : orderItems) {
            OrderItemEntity itemWithOrderId = OrderItemEntity.create(
                savedOrder.getId(),
                orderItem.getProductId(),
                orderItem.getProductOptionId(),
                orderItem.getProductName(),
                orderItem.getOptionType(),
                orderItem.getQuantity(),
                orderItem.getPrice()
            );
            OrderItemEntity savedItem = orderRepository.saveItem(itemWithOrderId);

            itemResponses.add(new OrderItemResponse(
                savedItem.getProductId(),
                savedItem.getProductOptionId(),
                savedItem.getProductName(),
                savedItem.getOptionType(),
                savedItem.getQuantity(),
                savedItem.getPrice()
            ));
        }

        return new OrderResponse(
            savedOrder.getId(),
            savedOrder.getUserId(),
            savedOrder.getTotalAmount(),
            savedOrder.getDiscountAmount(),
            savedOrder.getFinalAmount(),
            savedOrder.getStatus().name(),
            itemResponses,
            formatTimestamp(savedOrder.getCreatedAt())
        );
    }

    private String formatTimestamp(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .toString();
    }
}
