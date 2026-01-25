package com.hhplus.ecommerce.domain.productOption.event.kafka;

/**
 * 재고 이벤트 타입
 */
public enum StockEventType {
    STOCK_INCREASED,  // 재고 증가
    STOCK_DECREASED,  // 재고 감소
    STOCK_UPDATED     // 재고 설정
}
