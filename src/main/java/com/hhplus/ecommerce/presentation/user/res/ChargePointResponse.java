package com.hhplus.ecommerce.presentation.user.res;

public record ChargePointResponse(
    Long userId,
    Integer amount,
    Integer afterBalance,
    String transactionType,
    String chargedAt
) {
}
