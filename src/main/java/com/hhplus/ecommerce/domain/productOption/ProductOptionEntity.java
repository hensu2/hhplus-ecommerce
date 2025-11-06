package com.hhplus.ecommerce.domain.productOption;

import com.hhplus.ecommerce.common.exception.InvalidStockUpdateException;
import com.hhplus.ecommerce.presentation.productOption.res.StockOptionResponse;
import com.hhplus.ecommerce.presentation.productOption.res.UpdateStockResponse;

public record ProductOptionEntity(
    long id,
    long productId,
    String optionType,
    long additionalPrice,
    long stock,
    long createdAt,
    long updatedAt
) {
    public StockOptionResponse toStockOptionResponse() {
        return new StockOptionResponse(id, optionType, (int) stock, (int) additionalPrice);
    }

    public UpdateStockResponse toUpdateStockResponse() {
        return new UpdateStockResponse(id, optionType, stock, additionalPrice);
    }

    public ProductOptionEntity updateStock(StockUpdateType type, int amount) {
        long newStock = switch (type) {
            case SET -> amount;
            case INCREASE -> this.stock + amount;
            case DECREASE -> {
                long result = this.stock - amount;
                if (result < 0) {
                    throw new InvalidStockUpdateException("재고가 부족합니다. 현재 재고: " + this.stock);
                }
                yield result;
            }
        };

        return new ProductOptionEntity(
            this.id,
            this.productId,
            this.optionType,
            this.additionalPrice,
            newStock,
            this.createdAt,
            System.currentTimeMillis()
        );
    }
}
