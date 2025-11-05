package com.hhplus.ecommerce.presentation.order.req;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    private List<Long> cartItemIds;
    private Long couponHistoryId;
    private Integer usePoint;
}
