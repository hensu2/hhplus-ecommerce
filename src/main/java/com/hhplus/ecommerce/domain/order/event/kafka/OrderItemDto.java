package com.hhplus.ecommerce.domain.order.event.kafka;

import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 아이템 DTO (Kafka 직렬화용)
 * OrderItemEntity를 직접 Kafka로 보내지 않고 DTO로 변환
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemDto {
    private Long productId;
    private Long productOptionId;
    private String productName;
    private String optionType;
    private Integer quantity;
    private Integer price;

    /**
     * OrderItemEntity에서 DTO로 변환
     */
    public static OrderItemDto from(OrderItemEntity entity) {
        return new OrderItemDto(
            entity.getProductId(),
            entity.getProductOptionId(),
            entity.getProductName(),
            entity.getOptionType(),
            entity.getQuantity(),
            entity.getPrice()
        );
    }
}
