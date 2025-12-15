package com.hhplus.ecommerce.application.event;

import com.hhplus.ecommerce.domain.event.FailedEventEntity;
import com.hhplus.ecommerce.domain.event.FailedEventStatus;
import com.hhplus.ecommerce.infrastructure.event.FailedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 실패한 이벤트를 주기적으로 자동 재처리하는 스케줄러
 * 5분마다 실행되어 PENDING 상태의 이벤트를 재처리 시도
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FailedEventScheduler {

    private static final int MAX_RETRY_COUNT = 5; // 최대 재시도 횟수

    private final FailedEventRepository failedEventRepository;
    private final RetryFailedEventUseCase retryFailedEventUseCase;

    /**
     * 5분마다 실패 이벤트 자동 재처리
     * initialDelay = 60000: 애플리케이션 시작 1분 후 첫 실행
     * fixedDelay = 300000: 이전 실행 완료 후 5분 뒤 다음 실행
     */
    @Scheduled(initialDelay = 60000, fixedDelay = 300000)
    public void retryFailedEvents() {
        log.info("실패 이벤트 자동 재처리 시작");

        List<FailedEventEntity> pendingEvents = failedEventRepository.findByStatus(FailedEventStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            log.info("재처리할 실패 이벤트 없음");
            return;
        }

        log.info("재처리할 실패 이벤트 개수: {}", pendingEvents.size());

        int successCount = 0;
        int failedCount = 0;
        int maxRetryExceeded = 0;

        for (FailedEventEntity event : pendingEvents) {
            try {
                // 최대 재시도 횟수 초과 확인
                if (event.getRetryCount() >= MAX_RETRY_COUNT) {
                    log.warn("최대 재시도 횟수 초과 - eventId: {}, retryCount: {}",
                        event.getId(), event.getRetryCount());
                    event.markAsFailed("최대 재시도 횟수 초과 (MAX: " + MAX_RETRY_COUNT + ")");
                    failedEventRepository.save(event);
                    maxRetryExceeded++;
                    continue;
                }

                // 재처리 시도
                retryFailedEventUseCase.execute(event.getId());
                successCount++;

            } catch (Exception e) {
                log.error("자동 재처리 실패 - eventId: {}, retryCount: {}",
                    event.getId(), event.getRetryCount(), e);
                failedCount++;
            }
        }

        log.info("실패 이벤트 자동 재처리 완료 - 성공: {}, 실패: {}, 최대 재시도 초과: {}",
            successCount, failedCount, maxRetryExceeded);
    }
}
