package com.hhplus.ecommerce.application.order;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.domain.productOption.StockUpdateType;
import com.hhplus.ecommerce.infrastructure.order.OrderRepository;
import com.hhplus.ecommerce.infrastructure.productOption.ProductOptionRepository;
import com.hhplus.ecommerce.presentation.order.req.CreateOrderRequest;
import com.hhplus.ecommerce.presentation.order.req.OrderItemRequest;
import com.hhplus.ecommerce.presentation.order.res.OrderItemResponse;
import com.hhplus.ecommerce.presentation.order.res.OrderResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final ProductOptionRepository productOptionRepository;

    public CreateOrderUseCase(OrderRepository orderRepository, ProductOptionRepository productOptionRepository) {
        this.orderRepository = orderRepository;
        this.productOptionRepository = productOptionRepository;
    }

    public OrderResponse execute(CreateOrderRequest request) {
        long now = System.currentTimeMillis();

        // 1. 주문 아이템 검증 및 재고 차감
        List<OrderItemEntity> orderItems = new ArrayList<>();
        int totalAmount = 0;

        for (OrderItemRequest item : request.getItems()) {
            ProductOptionEntity option = productOptionRepository.findById(item.getProductOptionId())
                .orElseThrow(() -> new IllegalArgumentException("상품 옵션을 찾을 수 없습니다."));

            // 재고 차감
            ProductOptionEntity updatedOption = option.updateStock(StockUpdateType.DECREASE, item.getQuantity());
            productOptionRepository.save(updatedOption);

            int itemPrice = (int) (option.additionalPrice() * item.getQuantity());
            totalAmount += itemPrice;

            orderItems.add(new OrderItemEntity(
                0L,
                0L, // orderId는 주문 생성 후 설정
                option.productId(),
                option.id(),
                "상품명", // TODO: 상품 정보에서 가져와야 함
                option.optionType(),
                item.getQuantity(),
                itemPrice,
                now
            ));
        }

        // 2. 쿠폰 할인 적용
        int discountAmount = 0;
        // TODO: 쿠폰 할인 로직 구현

        int finalAmount = totalAmount - discountAmount;

        // 3. 주문 생성
        OrderEntity order = new OrderEntity(
            0L,
            request.getUserId(),
            totalAmount,
            discountAmount,
            finalAmount,
            request.getCouponHistoryId(),
            OrderStatus.COMPLETED,
            now,
            now
        );
        OrderEntity savedOrder = orderRepository.save(order);

        // 4. 주문 아이템 저장
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

        // 5. 응답 생성
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
