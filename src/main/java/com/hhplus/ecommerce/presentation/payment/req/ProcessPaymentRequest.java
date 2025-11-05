package com.hhplus.ecommerce.presentation.payment.req;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentRequest {
    private Long orderId;
    private String paymentMethod;
}
