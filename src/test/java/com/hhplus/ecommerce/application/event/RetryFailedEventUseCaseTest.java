package com.hhplus.ecommerce.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.application.salesRanking.SalesRankingService;
import com.hhplus.ecommerce.domain.event.EventType;
import com.hhplus.ecommerce.domain.event.FailedEventEntity;
import com.hhplus.ecommerce.domain.event.FailedEventStatus;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import com.hhplus.ecommerce.domain.order.event.OrderCreatedEvent;
import com.hhplus.ecommerce.infrastructure.event.FailedEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryFailedEventUseCaseTest {

    @Mock
    private FailedEventRepository failedEventRepository;

    @Mock
    private SalesRankingService salesRankingService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RetryFailedEventUseCase retryFailedEventUseCase;

    @Test
    @DisplayName("실패 이벤트 재처리 성공")
    void retryFailedEvent_success() throws Exception {
        // given
        Long eventId = 1L;
        Long orderId = 100L;
        Long orderedAt = System.currentTimeMillis();

        OrderItemEntity item = new OrderItemEntity(1L, orderId, 101L, 1L, "상품1", "옵션1", 2, 10000, orderedAt);
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, List.of(item), orderedAt);

        FailedEventEntity failedEvent = new FailedEventEntity(
            EventType.ORDER_CREATED,
            orderId,
            "{\"orderId\":100}",
            "Redis connection failed"
        );

        when(failedEventRepository.getOrThrow(eventId)).thenReturn(failedEvent);
        when(objectMapper.readValue(anyString(), eq(OrderCreatedEvent.class))).thenReturn(event);
        when(failedEventRepository.save(any(FailedEventEntity.class))).thenReturn(failedEvent);

        // when
        FailedEventEntity result = retryFailedEventUseCase.execute(eventId);

        // then
        verify(salesRankingService).increaseRanking(eq(101L), eq(2), eq(orderedAt));
        verify(failedEventRepository, times(2)).save(any(FailedEventEntity.class));
    }

    @Test
    @DisplayName("이미 성공한 이벤트는 재처리하지 않음")
    void retryFailedEvent_alreadySuccess() {
        // given
        Long eventId = 1L;
        FailedEventEntity successEvent = new FailedEventEntity(
            EventType.ORDER_CREATED,
            100L,
            "{\"orderId\":100}",
            null
        );
        successEvent.markAsSuccess();

        when(failedEventRepository.getOrThrow(eventId)).thenReturn(successEvent);

        // when & then
        assertThatThrownBy(() -> retryFailedEventUseCase.execute(eventId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("이미 성공한 이벤트입니다");
    }
}