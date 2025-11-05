package com.hhplus.ecommerce.presentation.user.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PointHistoryItemResponse {
    private Long id;
    private Integer amount;
    private String transactionType;
    private String description;
    private String createdAt;
}
