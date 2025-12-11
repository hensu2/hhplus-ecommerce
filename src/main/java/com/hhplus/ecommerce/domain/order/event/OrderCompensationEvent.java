package com.hhplus.ecommerce.domain.order.event;

import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 주문 보상 트랜잭션 이벤트
 * 판매 랭킹 업데이트 실패 시 발행되어 재고 복구 및 주문 취소를 트리거
 */
@Getter
@AllArgsConstructor
public class OrderCompensationEvent {
    private Long orderId;
    private List<OrderItemEntity> orderItems;
    private String failureReason;
    private Long timestamp;
}