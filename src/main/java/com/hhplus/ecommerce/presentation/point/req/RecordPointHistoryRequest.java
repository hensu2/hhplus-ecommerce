package com.hhplus.ecommerce.presentation.point.req;

public record RecordPointHistoryRequest(
        Long userId,
        Long amount,
        String transactionType,  // EARN, USE, REFUND
        String description
) {
}