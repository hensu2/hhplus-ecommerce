package com.hhplus.ecommerce.domain.event;

public enum EventType {
    ORDER_CREATED,    // 주문 생성 이벤트
    ORDER_CANCELLED,  // 주문 취소 이벤트
    STOCK_INCREASED,  // 재고 증가 이벤트
    STOCK_DECREASED,  // 재고 감소 이벤트
    STOCK_UPDATED     // 재고 설정 이벤트
}