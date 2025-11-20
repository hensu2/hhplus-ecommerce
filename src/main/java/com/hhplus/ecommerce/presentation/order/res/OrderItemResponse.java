package com.hhplus.ecommerce.presentation.order.res;

import com.hhplus.ecommerce.domain.order.OrderItemEntity;

public record OrderItemResponse(
        Long productId,
        Long productOptionId,
        String productName,
        String optionType,
        Integer quantity,
        Integer price
) {
    public OrderItemResponse(OrderItemEntity item) {
        this(
            item.getProductId(),
            item.getProductOptionId(),
            item.getProductName(),
            item.getOptionType(),
            item.getQuantity(),
            item.getPrice()
        );
    }
}
