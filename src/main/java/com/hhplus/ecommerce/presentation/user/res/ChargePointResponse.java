package com.hhplus.ecommerce.presentation.user.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChargePointResponse {
    private Long userId;
    private Integer amount;
    private Integer afterBalance;
    private String transactionType;
    private String chargedAt;
}
