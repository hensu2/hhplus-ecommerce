package com.hhplus.ecommerce.presentation.event;

import com.hhplus.ecommerce.application.event.GetFailedEventsUseCase;
import com.hhplus.ecommerce.application.event.RetryFailedEventUseCase;
import com.hhplus.ecommerce.domain.event.FailedEventEntity;
import com.hhplus.ecommerce.domain.event.FailedEventStatus;
import com.hhplus.ecommerce.presentation.event.res.FailedEventListResponse;
import com.hhplus.ecommerce.presentation.event.res.FailedEventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "이벤트", description = "실패 이벤트 관리 API")
@RestController
@RequestMapping("/api/events/failed")
@RequiredArgsConstructor
public class EventController {

    private final GetFailedEventsUseCase getFailedEventsUseCase;
    private final RetryFailedEventUseCase retryFailedEventUseCase;

    @Operation(summary = "실패 이벤트 목록 조회", description = "모든 실패 이벤트 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<FailedEventListResponse> getFailedEvents(
            @RequestParam(required = false) String status
    ) {
        List<FailedEventEntity> events;

        if (status != null) {
            FailedEventStatus eventStatus = FailedEventStatus.valueOf(status.toUpperCase());
            events = getFailedEventsUseCase.executeByStatus(eventStatus);
        } else {
            events = getFailedEventsUseCase.execute();
        }

        return ResponseEntity.ok(new FailedEventListResponse(events));
    }

    @Operation(summary = "실패 이벤트 상세 조회", description = "특정 실패 이벤트의 상세 정보를 조회합니다.")
    @GetMapping("/{eventId}")
    public ResponseEntity<FailedEventResponse> getFailedEvent(@PathVariable Long eventId) {
        FailedEventEntity event = getFailedEventsUseCase.executeById(eventId);
        return ResponseEntity.ok(new FailedEventResponse(event));
    }

    @Operation(summary = "실패 이벤트 재처리", description = "특정 실패 이벤트를 재처리합니다.")
    @PostMapping("/{eventId}/retry")
    public ResponseEntity<FailedEventResponse> retryFailedEvent(@PathVariable Long eventId) {
        FailedEventEntity event = retryFailedEventUseCase.execute(eventId);
        return ResponseEntity.ok(new FailedEventResponse(event));
    }
}