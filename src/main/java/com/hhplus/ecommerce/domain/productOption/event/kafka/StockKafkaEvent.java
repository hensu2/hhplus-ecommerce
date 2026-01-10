package com.hhplus.ecommerce.domain.productOption.event.kafka;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 재고 Kafka 이벤트 추상 클래스
 * 모든 재고 관련 Kafka 이벤트의 부모 클래스
 */
@Getter
@NoArgsConstructor
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "eventType"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = StockChangedKafkaEvent.class, name = "STOCK_INCREASED"),
    @JsonSubTypes.Type(value = StockChangedKafkaEvent.class, name = "STOCK_DECREASED"),
    @JsonSubTypes.Type(value = StockChangedKafkaEvent.class, name = "STOCK_UPDATED")
})
public abstract class StockKafkaEvent {
    protected StockEventType eventType;
    protected Long productOptionId;
    protected Long timestamp;

    protected StockKafkaEvent(StockEventType eventType, Long productOptionId, Long timestamp) {
        this.eventType = eventType;
        this.productOptionId = productOptionId;
        this.timestamp = timestamp;
    }
}
