package com.hhplus.ecommerce.application.order;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.infrastructure.order.OrderRepository;
import com.hhplus.ecommerce.presentation.order.res.OrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelOrderUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CancelOrderUseCase cancelOrderUseCase;

    private OrderEntity pendingOrder;
    private long orderId;

    @BeforeEach
    void setUp() {
        orderId = 1L;
        long now = System.currentTimeMillis();

        pendingOrder = new OrderEntity(
            orderId,
            1L,
            30000,
            0,
            30000,
            null,
            OrderStatus.PENDING,
            now,
            now,
            now
        );
    }

    @Test
    @DisplayName("주문 취소에 성공한다")
    void cancelOrder() {
        // given
        when(orderRepository.getOrThrow(orderId)).thenReturn(pendingOrder);
        when(orderRepository.findItemsByOrderId(orderId)).thenReturn(Collections.emptyList());

        long now = System.currentTimeMillis();
        OrderEntity cancelledOrder = new OrderEntity(
            orderId,
            pendingOrder.getUserId(),
            pendingOrder.getTotalAmount(),
            pendingOrder.getDiscountAmount(),
            pendingOrder.getFinalAmount(),
            pendingOrder.getCouponHistoryId(),
            OrderStatus.CANCELLED,
            pendingOrder.getOrderedAt(),
            pendingOrder.getCreatedAt(),
            now
        );
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(cancelledOrder);

        // when
        OrderEntity result = cancelOrderUseCase.execute(orderId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(orderId);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("존재하지 않는 주문 ID로 취소 시도 시 예외를 발생시킨다")
    void cancelOrderWithInvalidId() {
        // given
        when(orderRepository.getOrThrow(999L))
            .thenThrow(new IllegalArgumentException("주문을 찾을 수 없습니다."));

        // when & then
        assertThatThrownBy(() -> cancelOrderUseCase.execute(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("주문을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("이미 취소된 주문을 다시 취소 시도 시 예외를 발생시킨다")
    void cancelAlreadyCancelledOrder() {
        // given
        long now = System.currentTimeMillis();
        OrderEntity cancelledOrder = new OrderEntity(
            orderId,
            1L,
            30000,
            0,
            30000,
            null,
            OrderStatus.CANCELLED,
            now,
            now,
            now
        );
        when(orderRepository.getOrThrow(orderId)).thenReturn(cancelledOrder);

        // when & then
        assertThatThrownBy(() -> cancelOrderUseCase.execute(orderId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("이미 취소된 주문입니다.");
    }
}
