package com.hhplus.ecommerce.presentation.order.req;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderRequest {
    private String reason;
}
