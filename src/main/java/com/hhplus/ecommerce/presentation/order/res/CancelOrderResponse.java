package com.hhplus.ecommerce.presentation.order.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderResponse {
    private Long orderId;
    private String status;
    private Integer refundAmount;
    private Integer refundPoint;
    private Boolean couponRestored;
    private String cancelledAt;
}
