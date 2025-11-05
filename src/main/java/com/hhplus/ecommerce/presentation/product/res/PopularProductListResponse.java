package com.hhplus.ecommerce.presentation.product.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PopularProductListResponse {
    private List<PopularProductResponse> products;
    private String period;
    private String generatedAt;
}
