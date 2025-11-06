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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompleteOrderUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private CompleteOrderUseCase completeOrderUseCase;

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
            now
        );
    }

    @Test
    @DisplayName("주문 완료에 성공한다")
    void completeOrder() {
        // given
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));

        long now = System.currentTimeMillis();
        OrderEntity completedOrder = new OrderEntity(
            orderId,
            pendingOrder.userId(),
            pendingOrder.totalAmount(),
            pendingOrder.discountAmount(),
            pendingOrder.finalAmount(),
            pendingOrder.couponHistoryId(),
            OrderStatus.COMPLETED,
            pendingOrder.createdAt(),
            now
        );
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(completedOrder);

        // when
        OrderResponse response = completeOrderUseCase.execute(orderId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("존재하지 않는 주문 ID로 완료 시도 시 예외를 발생시킨다")
    void completeOrderWithInvalidId() {
        // given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> completeOrderUseCase.execute(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("주문 정보를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("이미 완료된 주문을 다시 완료 시도 시 예외를 발생시킨다")
    void completeAlreadyCompletedOrder() {
        // given
        long now = System.currentTimeMillis();
        OrderEntity completedOrder = new OrderEntity(
            orderId,
            1L,
            30000,
            0,
            30000,
            null,
            OrderStatus.COMPLETED,
            now,
            now
        );
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(completedOrder));

        // when & then
        assertThatThrownBy(() -> completeOrderUseCase.execute(orderId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("이미 완료된 주문입니다.");
    }

    @Test
    @DisplayName("취소된 주문은 완료할 수 없다")
    void completeCannotBeDoneForCancelledOrder() {
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
            now
        );
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(cancelledOrder));

        // when & then
        assertThatThrownBy(() -> completeOrderUseCase.execute(orderId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("취소된 주문은 완료할 수 없습니다.");
    }
}
