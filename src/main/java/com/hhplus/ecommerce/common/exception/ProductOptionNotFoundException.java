package com.hhplus.ecommerce.common.exception;

public class ProductOptionNotFoundException extends RuntimeException {
    public ProductOptionNotFoundException(String message) {
        super(message);
    }
}