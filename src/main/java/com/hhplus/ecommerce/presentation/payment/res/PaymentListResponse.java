package com.hhplus.ecommerce.presentation.payment.res;

import java.util.List;

public record PaymentListResponse(
    List<PaymentResponse> payments
) {
}
