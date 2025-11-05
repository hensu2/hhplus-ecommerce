package com.hhplus.ecommerce.presentation.product.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResponse {
    private Long id;
    private String productName;
    private String content;
    private Integer price;
    private List<ProductOptionResponse> options;
    private String createdAt;
}
