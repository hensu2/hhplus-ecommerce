package com.hhplus.ecommerce.application.order;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.domain.order.OrderRepository;
import com.hhplus.ecommerce.presentation.order.res.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompleteOrderUseCase {

    private final OrderRepository orderRepository;

    public OrderResponse execute(long orderId) {
        // 1. 주문 정보 조회
        OrderEntity order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("주문 정보를 찾을 수 없습니다."));

        // 2. 이미 완료된 주문인지 확인
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("이미 완료된 주문입니다.");
        }

        // 3. 취소된 주문은 완료할 수 없음
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("취소된 주문은 완료할 수 없습니다.");
        }

        // 4. 주문 완료 처리 (결제 후)
        order.complete();
        OrderEntity savedOrder = orderRepository.save(order);

        // 5. 응답 생성
        return new OrderResponse(
            savedOrder.getId(),
            savedOrder.getUserId(),
            savedOrder.getTotalAmount(),
            savedOrder.getDiscountAmount(),
            savedOrder.getFinalAmount(),
            savedOrder.getStatus().name(),
            List.of(), // 주문 아이템은 간단히 빈 리스트로 처리
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
