package com.hhplus.ecommerce.presentation.productOption.req;

import com.hhplus.ecommerce.domain.productOption.StockUpdateType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStockRequest {
    @NotNull(message = "재고 업데이트 타입은 필수입니다.")
    private StockUpdateType type;

    @NotNull(message = "재고 수량은 필수입니다.")
    @PositiveOrZero(message = "재고 수량은 0 이상이어야 합니다.")
    private Integer amount;
}
