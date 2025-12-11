package com.hhplus.ecommerce.application.order;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.infrastructure.order.OrderRepository;
import com.hhplus.ecommerce.infrastructure.productOption.ProductOptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SAGA 보상 트랜잭션 UseCase 테스트")
class CompensateOrderUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductOptionRepository productOptionRepository;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @InjectMocks
    private CompensateOrderUseCase compensateOrderUseCase;

    @Test
    @DisplayName("SAGA 보상 트랜잭션 - 주문 취소 및 재고 복구 성공")
    void compensateOrder_success() throws Exception {
        // given
        Long orderId = 1L;
        Long productOptionId = 1L;
        Integer quantity = 2;

        OrderEntity order = new OrderEntity(
            orderId, 1L, 30000, 0, 30000, null,
            OrderStatus.PENDING, 0L, 0L, 0L
        );

        OrderItemEntity orderItem = new OrderItemEntity(
            1L, orderId, 101L, productOptionId, "테스트 상품", "Red", quantity, 20000, 0L
        );

        ProductOptionEntity productOption = new ProductOptionEntity(
            productOptionId, 101L, "Red", 1000L, 10L, 0L, 0L
        );

        when(orderRepository.getOrThrow(orderId)).thenReturn(order);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(redissonClient.getMultiLock(any())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(productOptionRepository.getOrThrow(productOptionId)).thenReturn(productOption);
        when(productOptionRepository.save(any(ProductOptionEntity.class))).thenReturn(productOption);
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        OrderEntity result = compensateOrderUseCase.execute(orderId, List.of(orderItem));

        // then
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(productOptionRepository).getOrThrow(productOptionId);
        verify(productOptionRepository).save(any(ProductOptionEntity.class));
        verify(orderRepository).save(any(OrderEntity.class));
    }

    @Test
    @DisplayName("SAGA 보상 트랜잭션 - 이미 취소된 주문은 보상 생략")
    void compensateOrder_alreadyCancelled() throws Exception {
        // given
        Long orderId = 1L;

        OrderEntity cancelledOrder = new OrderEntity(
            orderId, 1L, 30000, 0, 30000, null,
            OrderStatus.CANCELLED, 0L, 0L, 0L
        );

        when(orderRepository.getOrThrow(orderId)).thenReturn(cancelledOrder);

        // when
        OrderEntity result = compensateOrderUseCase.execute(orderId, List.of());

        // then
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(productOptionRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }
}