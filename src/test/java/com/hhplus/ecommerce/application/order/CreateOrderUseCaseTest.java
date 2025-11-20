package com.hhplus.ecommerce.application.order;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.infrastructure.order.OrderRepository;
import com.hhplus.ecommerce.infrastructure.product.ProductRepository;
import com.hhplus.ecommerce.infrastructure.productOption.ProductOptionRepository;
import com.hhplus.ecommerce.presentation.order.req.CreateOrderRequest;
import com.hhplus.ecommerce.presentation.order.req.OrderItemRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateOrderUseCase 단위 테스트")
class CreateOrderUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductOptionRepository productOptionRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CreateOrderUseCase createOrderUseCase;

    @Test
    @DisplayName("주문 생성 성공")
    void execute_Success() {
        // given
        Long userId = 1L;
        Long productId = 1L;
        Long optionId = 1L;
        Integer quantity = 2;
        Long productPrice = 10000L;
        Long optionPrice = 1000L;

        ProductEntity product = new ProductEntity(productId, userId, "테스트 상품", "설명", productPrice, 0L, 0L);
        ProductOptionEntity option = new ProductOptionEntity(optionId, productId, "Red", optionPrice, 100L, 0L, 0L);

        OrderItemRequest itemRequest = new OrderItemRequest(optionId, quantity);
        CreateOrderRequest request = new CreateOrderRequest(userId, List.of(itemRequest), null);

        OrderEntity savedOrder = new OrderEntity(1L, userId, 22000, 0, 22000, null, OrderStatus.PENDING, 0L, 0L, 0L);

        when(productOptionRepository.getOrThrow(optionId)).thenReturn(option);
        when(productRepository.getOrThrow(productId)).thenReturn(product);
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedOrder);
        when(orderRepository.saveItem(any(OrderItemEntity.class))).thenReturn(any(OrderItemEntity.class));

        // when
        OrderEntity result = createOrderUseCase.execute(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository, times(1)).save(any(OrderEntity.class));
        verify(orderRepository, times(1)).saveItem(any(OrderItemEntity.class));
        verify(productOptionRepository, times(1)).decreaseStock(optionId, quantity);
    }
}
