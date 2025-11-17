package com.hhplus.ecommerce.presentation.product.res;

import com.hhplus.ecommerce.common.dto.PageResponse;
import lombok.Getter;

import java.util.List;

@Getter
public class ProductListResponse extends PageResponse {
    private List<ProductResponse> content;

    public ProductListResponse(List<ProductResponse> content, Integer totalElements, Integer totalPages, Integer size, Integer number) {
        super(totalElements, totalPages, size, number);
        this.content = content;
    }
}
