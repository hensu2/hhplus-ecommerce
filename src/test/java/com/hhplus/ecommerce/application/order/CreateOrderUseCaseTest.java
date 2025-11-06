package com.hhplus.ecommerce.application.order;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.domain.productOption.StockUpdateType;
import com.hhplus.ecommerce.infrastructure.order.OrderRepository;
import com.hhplus.ecommerce.infrastructure.productOption.ProductOptionRepository;
import com.hhplus.ecommerce.presentation.order.req.CreateOrderRequest;
import com.hhplus.ecommerce.presentation.order.req.OrderItemRequest;
import com.hhplus.ecommerce.presentation.order.res.OrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class CreateOrderUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductOptionRepository productOptionRepository;

    @InjectMocks
    private CreateOrderUseCase createOrderUseCase;

    private ProductOptionEntity testOption1;
    private ProductOptionEntity testOption2;
    private long userId;

    @BeforeEach
    void setUp() {
        long now = System.currentTimeMillis();
        userId = 1L;

        testOption1 = new ProductOptionEntity(
            1L,
            1L,
            "색상:블랙",
            10000L,
            100L,
            now,
            now
        );

        testOption2 = new ProductOptionEntity(
            2L,
            1L,
            "색상:화이트",
            10000L,
            50L,
            now,
            now
        );
    }

    @Test
    @DisplayName("주문 생성에 성공한다")
    void createOrder() {
        // given
        OrderItemRequest item1 = new OrderItemRequest(1L, 2);
        OrderItemRequest item2 = new OrderItemRequest(2L, 1);
        CreateOrderRequest request = new CreateOrderRequest(
            userId,
            Arrays.asList(item1, item2),
            null
        );

        ProductOptionEntity decreasedOption1 = testOption1.updateStock(StockUpdateType.DECREASE, 2);
        ProductOptionEntity decreasedOption2 = testOption2.updateStock(StockUpdateType.DECREASE, 1);

        when(productOptionRepository.findById(1L)).thenReturn(Optional.of(testOption1));
        when(productOptionRepository.findById(2L)).thenReturn(Optional.of(testOption2));
        when(productOptionRepository.decreaseStock(1L, 2)).thenReturn(decreasedOption1);
        when(productOptionRepository.decreaseStock(2L, 1)).thenReturn(decreasedOption2);

        long now = System.currentTimeMillis();
        OrderEntity savedOrder = new OrderEntity(
            1L,
            userId,
            30000,
            0,
            30000,
            null,
            OrderStatus.COMPLETED,
            now,
            now
        );
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedOrder);
        when(orderRepository.saveItem(any(OrderItemEntity.class)))
            .thenAnswer(invocation -> {
                OrderItemEntity item = invocation.getArgument(0);
                return new OrderItemEntity(1L, item.orderId(), item.productId(), item.productOptionId(),
                    item.productName(), item.optionType(), item.quantity(), item.price(), item.createdAt());
            });

        // when
        OrderResponse response = createOrderUseCase.execute(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getTotalAmount()).isEqualTo(30000);
        assertThat(response.getFinalAmount()).isEqualTo(30000);
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("존재하지 않는 상품 옵션으로 주문 시도 시 예외를 발생시킨다")
    void createOrderWithInvalidOption() {
        // given
        OrderItemRequest item = new OrderItemRequest(999L, 1);
        CreateOrderRequest request = new CreateOrderRequest(
            userId,
            Arrays.asList(item),
            null
        );

        when(productOptionRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> createOrderUseCase.execute(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("상품 옵션을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("재고 부족 시 주문 생성에 실패한다")
    void createOrderWithInsufficientStock() {
        // given
        OrderItemRequest item = new OrderItemRequest(1L, 200); // 재고보다 많은 수량
        CreateOrderRequest request = new CreateOrderRequest(
            userId,
            Arrays.asList(item),
            null
        );

        when(productOptionRepository.findById(1L)).thenReturn(Optional.of(testOption1));
        when(productOptionRepository.decreaseStock(1L, 200))
            .thenThrow(new com.hhplus.ecommerce.common.exception.InvalidStockUpdateException("재고가 부족합니다. 현재 재고: 100"));

        // when & then
        assertThatThrownBy(() -> createOrderUseCase.execute(request))
            .hasMessageContaining("재고가 부족합니다");
    }

    @Test
    @DisplayName("주문 생성 시 재고가 차감된다")
    void createOrderDecreasesStock() {
        // given
        OrderItemRequest item = new OrderItemRequest(1L, 2);
        CreateOrderRequest request = new CreateOrderRequest(
            userId,
            Arrays.asList(item),
            null
        );

        ProductOptionEntity decreasedOption = testOption1.updateStock(StockUpdateType.DECREASE, 2);

        when(productOptionRepository.findById(1L)).thenReturn(Optional.of(testOption1));
        when(productOptionRepository.decreaseStock(1L, 2)).thenReturn(decreasedOption);

        long now = System.currentTimeMillis();
        OrderEntity savedOrder = new OrderEntity(
            1L,
            userId,
            20000,
            0,
            20000,
            null,
            OrderStatus.COMPLETED,
            now,
            now
        );
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedOrder);
        when(orderRepository.saveItem(any(OrderItemEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        createOrderUseCase.execute(request);

        // then
        verify(productOptionRepository).decreaseStock(1L, 2);
    }

    @Test
    @DisplayName("여러 상품 옵션으로 주문 생성이 가능하다")
    void createOrderWithMultipleItems() {
        // given
        OrderItemRequest item1 = new OrderItemRequest(1L, 1);
        OrderItemRequest item2 = new OrderItemRequest(2L, 2);
        CreateOrderRequest request = new CreateOrderRequest(
            userId,
            Arrays.asList(item1, item2),
            null
        );

        ProductOptionEntity decreasedOption1 = testOption1.updateStock(StockUpdateType.DECREASE, 1);
        ProductOptionEntity decreasedOption2 = testOption2.updateStock(StockUpdateType.DECREASE, 2);

        when(productOptionRepository.findById(1L)).thenReturn(Optional.of(testOption1));
        when(productOptionRepository.findById(2L)).thenReturn(Optional.of(testOption2));
        when(productOptionRepository.decreaseStock(1L, 1)).thenReturn(decreasedOption1);
        when(productOptionRepository.decreaseStock(2L, 2)).thenReturn(decreasedOption2);

        long now = System.currentTimeMillis();
        OrderEntity savedOrder = new OrderEntity(
            1L,
            userId,
            30000,
            0,
            30000,
            null,
            OrderStatus.COMPLETED,
            now,
            now
        );
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedOrder);
        when(orderRepository.saveItem(any(OrderItemEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        OrderResponse response = createOrderUseCase.execute(request);

        // then
        assertThat(response.getItems()).hasSize(2);
        verify(productOptionRepository).decreaseStock(1L, 1);
        verify(productOptionRepository).decreaseStock(2L, 2);
        verify(orderRepository, times(2)).saveItem(any(OrderItemEntity.class));
    }
}
