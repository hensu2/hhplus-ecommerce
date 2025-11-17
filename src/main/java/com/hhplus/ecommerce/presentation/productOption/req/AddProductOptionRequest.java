package com.hhplus.ecommerce.presentation.productOption.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddProductOptionRequest(
    @NotNull(message = "상품 ID는 필수입니다.")
    @Min(value = 1, message = "상품 ID는 1 이상이어야 합니다.")
    Long productId,

    @NotBlank(message = "옵션 타입은 필수입니다.")
    String optionType,

    @NotNull(message = "추가 가격은 필수입니다.")
    @Min(value = 0, message = "추가 가격은 0 이상이어야 합니다.")
    Long additionalPrice,

    @NotNull(message = "재고는 필수입니다.")
    @Min(value = 0, message = "재고는 0 이상이어야 합니다.")
    Long stock
) {
}