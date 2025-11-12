package com.hhplus.ecommerce.presentation.order.res;

import com.hhplus.ecommerce.common.dto.PageResponse;
import lombok.Getter;

import java.util.List;

@Getter
public class OrderListResponse extends PageResponse {
    private List<OrderListItemResponse> content;

    public OrderListResponse(List<OrderListItemResponse> content, Integer totalElements, Integer totalPages, Integer size, Integer number) {
        super(totalElements, totalPages, size, number);
        this.content = content;
    }
}
