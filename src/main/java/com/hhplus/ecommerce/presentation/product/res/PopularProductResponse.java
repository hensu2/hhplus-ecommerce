package com.hhplus.ecommerce.presentation.product.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PopularProductResponse {
    private Long productId;
    private String productName;
    private Integer price;
    private Long viewCount;
    private Long salesCount;
    private Long popularityScore;
}
