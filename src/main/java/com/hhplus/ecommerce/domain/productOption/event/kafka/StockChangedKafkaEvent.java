package com.hhplus.ecommerce.domain.productOption.event.kafka;

import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 재고 변경 Kafka 이벤트
 * 재고가 증가/감소/설정되었을 때 Kafka로 발행되는 이벤트
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StockChangedKafkaEvent extends StockKafkaEvent {
    private Long productId;
    private String productName;
    private String optionType;
    private Long previousStock;
    private Long currentStock;
    private Integer changeAmount;
    private String changeReason; // "ORDER_CREATED", "ORDER_CANCELLED", "ADMIN_UPDATE"

    public StockChangedKafkaEvent(
        StockEventType eventType,
        ProductOptionEntity productOption,
        String productName,
        Long previousStock,
        Long currentStock,
        Integer changeAmount,
        String changeReason
    ) {
        super(eventType, productOption.getId(), System.currentTimeMillis());
        this.productId = productOption.getProductId();
        this.productName = productName;
        this.optionType = productOption.getOptionType();
        this.previousStock = previousStock;
        this.currentStock = currentStock;
        this.changeAmount = changeAmount;
        this.changeReason = changeReason;
    }
}
