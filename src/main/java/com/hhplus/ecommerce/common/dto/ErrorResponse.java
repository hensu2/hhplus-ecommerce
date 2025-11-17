package com.hhplus.ecommerce.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String error;
    private String message;

    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message);
    }
}
