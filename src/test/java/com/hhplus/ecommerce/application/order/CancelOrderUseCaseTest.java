package com.hhplus.ecommerce.application.order;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.domain.productOption.StockUpdateType;
import com.hhplus.ecommerce.infrastructure.order.OrderRepository;
import com.hhplus.ecommerce.infrastructure.productOption.ProductOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelOrderUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductOptionRepository productOptionRepository;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RLock lock;

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
    @DisplayName("주문 취소에 성공하고 재고가 복구된다")
    void cancelOrder() throws Exception {
        // given
        Long productOptionId = 1L;
        Integer orderQuantity = 2;
        Long currentStock = 10L;

        OrderItemEntity orderItem = new OrderItemEntity(
            1L,
            orderId,
            1L,
            productOptionId,
            "테스트 상품",
            "Red",
            orderQuantity,
            20000,
            System.currentTimeMillis()
        );

        ProductOptionEntity productOption = new ProductOptionEntity(
            productOptionId,
            1L,
            "Red",
            1000L,
            currentStock,
            0L,
            0L
        );

        when(orderRepository.getOrThrow(orderId)).thenReturn(pendingOrder);
        when(orderRepository.findItemsByOrderId(orderId)).thenReturn(List.of(orderItem));

        // Mock Redisson lock
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(redissonClient.getMultiLock(any())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);

        // Mock TransactionTemplate to execute the callback
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

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
        when(productOptionRepository.getOrThrow(productOptionId)).thenReturn(productOption);
        when(productOptionRepository.save(any(ProductOptionEntity.class))).thenReturn(productOption);

        // when
        OrderEntity result = cancelOrderUseCase.execute(orderId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(orderId);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // 재고 복구 확인
        verify(productOptionRepository, times(1)).getOrThrow(productOptionId);
        verify(productOptionRepository, times(1)).save(any(ProductOptionEntity.class));
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
