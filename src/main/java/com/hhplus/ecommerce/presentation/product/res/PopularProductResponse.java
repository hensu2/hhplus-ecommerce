package com.hhplus.ecommerce.presentation.product.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PopularProductResponse {
    private Long id;
    private String productName;
    private Integer price;
    private Integer salesCount;
    private Integer ranking;
}
