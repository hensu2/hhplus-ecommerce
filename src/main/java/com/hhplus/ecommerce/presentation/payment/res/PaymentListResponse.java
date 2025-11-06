package com.hhplus.ecommerce.presentation.payment.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentListResponse {
    private List<PaymentResponse> payments;
}
