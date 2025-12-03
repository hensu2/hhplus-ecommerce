package com.hhplus.ecommerce.application.order;

import com.hhplus.ecommerce.application.salesRanking.SalesRankingService;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import com.hhplus.ecommerce.domain.order.event.OrderCancelledEvent;
import com.hhplus.ecommerce.domain.order.event.OrderCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock
    private SalesRankingService salesRankingService;

    @InjectMocks
    private OrderEventListener orderEventListener;

    @Test
    @DisplayName("주문 생성 이벤트 수신 - 모든 주문 아이템에 대해 랭킹 증가")
    void onOrderCreated() {
        // given
        Long orderId = 1L;
        Long orderedAt = System.currentTimeMillis();

        OrderItemEntity item1 = new OrderItemEntity(1L, orderId, 101L, 1L, "상품1", "옵션1", 2, 10000, orderedAt);
        OrderItemEntity item2 = new OrderItemEntity(2L, orderId, 102L, 2L, "상품2", "옵션2", 3, 20000, orderedAt);

        List<OrderItemEntity> orderItems = Arrays.asList(item1, item2);
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, orderItems, orderedAt);

        // when
        orderEventListener.onOrderCreated(event);

        // then
        verify(salesRankingService).increaseRanking(eq(101L), eq(2), eq(orderedAt));
        verify(salesRankingService).increaseRanking(eq(102L), eq(3), eq(orderedAt));
        verifyNoMoreInteractions(salesRankingService);
    }

    @Test
    @DisplayName("주문 생성 이벤트 수신 - 예외 발생 시 로깅만 수행")
    void onOrderCreated_exceptionHandling() {
        // given
        Long orderId = 1L;
        Long orderedAt = System.currentTimeMillis();

        OrderItemEntity item = new OrderItemEntity(1L, orderId, 101L, 1L, "상품1", "옵션1", 2, 10000, orderedAt);
        List<OrderItemEntity> orderItems = Arrays.asList(item);

        OrderCreatedEvent event = new OrderCreatedEvent(orderId, orderItems, orderedAt);

        doThrow(new RuntimeException("Redis connection failed"))
            .when(salesRankingService).increaseRanking(anyLong(), anyInt(), anyLong());

        // when & then (예외가 발생해도 메서드는 정상 종료되어야 함)
        orderEventListener.onOrderCreated(event);

        verify(salesRankingService).increaseRanking(eq(101L), eq(2), eq(orderedAt));
    }

    @Test
    @DisplayName("주문 취소 이벤트 수신 - 모든 주문 아이템에 대해 랭킹 감소")
    void onOrderCancelled() {
        // given
        Long orderId = 2L;
        Long cancelledAt = System.currentTimeMillis();

        OrderItemEntity item1 = new OrderItemEntity(3L, orderId, 201L, 3L, "상품3", "옵션3", 1, 15000, cancelledAt);
        OrderItemEntity item2 = new OrderItemEntity(4L, orderId, 202L, 4L, "상품4", "옵션4", 5, 25000, cancelledAt);

        List<OrderItemEntity> orderItems = Arrays.asList(item1, item2);
        OrderCancelledEvent event = new OrderCancelledEvent(orderId, orderItems, cancelledAt);

        // when
        orderEventListener.onOrderCancelled(event);

        // then
        verify(salesRankingService).decreaseRanking(eq(201L), eq(1), eq(cancelledAt));
        verify(salesRankingService).decreaseRanking(eq(202L), eq(5), eq(cancelledAt));
        verifyNoMoreInteractions(salesRankingService);
    }

    @Test
    @DisplayName("주문 취소 이벤트 수신 - 예외 발생 시 로깅만 수행")
    void onOrderCancelled_exceptionHandling() {
        // given
        Long orderId = 2L;
        Long cancelledAt = System.currentTimeMillis();

        OrderItemEntity item = new OrderItemEntity(3L, orderId, 201L, 3L, "상품3", "옵션3", 1, 15000, cancelledAt);
        List<OrderItemEntity> orderItems = Arrays.asList(item);

        OrderCancelledEvent event = new OrderCancelledEvent(orderId, orderItems, cancelledAt);

        doThrow(new RuntimeException("Redis connection failed"))
            .when(salesRankingService).decreaseRanking(anyLong(), anyInt(), anyLong());

        // when & then (예외가 발생해도 메서드는 정상 종료되어야 함)
        orderEventListener.onOrderCancelled(event);

        verify(salesRankingService).decreaseRanking(eq(201L), eq(1), eq(cancelledAt));
    }

    @Test
    @DisplayName("빈 주문 아이템 리스트 처리")
    void onOrderCreated_emptyItems() {
        // given
        Long orderId = 3L;
        Long orderedAt = System.currentTimeMillis();

        OrderCreatedEvent event = new OrderCreatedEvent(orderId, Arrays.asList(), orderedAt);

        // when
        orderEventListener.onOrderCreated(event);

        // then
        verifyNoInteractions(salesRankingService);
    }
}