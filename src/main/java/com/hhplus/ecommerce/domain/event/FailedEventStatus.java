package com.hhplus.ecommerce.domain.event;

public enum FailedEventStatus {
    PENDING,      // 재처리 대기 중
    PROCESSING,   // 재처리 진행 중
    SUCCESS,      // 재처리 성공
    FAILED        // 재처리 실패 (최종)
}