package com.hhplus.ecommerce.presentation.product.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductOptionResponse {
    private Long id;
    private String optionType;
    private Integer additionalPrice;
    private Integer stock;
}
