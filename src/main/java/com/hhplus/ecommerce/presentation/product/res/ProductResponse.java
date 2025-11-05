package com.hhplus.ecommerce.presentation.product.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String productName;
    private String content;
    private Integer price;
    private String createdAt;
}
