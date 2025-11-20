package com.hhplus.ecommerce.presentation.order;

import com.hhplus.ecommerce.application.order.CancelOrderUseCase;
import com.hhplus.ecommerce.application.order.CompleteOrderUseCase;
import com.hhplus.ecommerce.application.order.CreateOrderUseCase;
import com.hhplus.ecommerce.application.order.GetOrderItemsUseCase;
import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import com.hhplus.ecommerce.presentation.order.req.CreateOrderRequest;
import com.hhplus.ecommerce.presentation.order.res.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "주문", description = "주문 관리 API")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final CompleteOrderUseCase completeOrderUseCase;
    private final GetOrderItemsUseCase getOrderItemsUseCase;

    @Operation(summary = "주문 생성", description = "상품을 주문합니다. 재고 차감 및 쿠폰 적용이 포함됩니다.")
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        OrderEntity order = createOrderUseCase.execute(request);
        List<OrderItemEntity> items = getOrderItemsUseCase.execute(order.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new OrderResponse(order, items));
    }

    @Operation(summary = "주문 취소", description = "주문을 취소합니다.")
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long orderId) {
        OrderEntity order = cancelOrderUseCase.execute(orderId);
        List<OrderItemEntity> items = getOrderItemsUseCase.execute(orderId);
        return ResponseEntity.ok(new OrderResponse(order, items));
    }

    @Operation(summary = "주문 완료", description = "결제 후 주문을 완료 상태로 변경합니다.")
    @PostMapping("/{orderId}/complete")
    public ResponseEntity<OrderResponse> completeOrder(@PathVariable Long orderId) {
        OrderEntity order = completeOrderUseCase.execute(orderId);
        List<OrderItemEntity> items = getOrderItemsUseCase.execute(orderId);
        return ResponseEntity.ok(new OrderResponse(order, items));
    }
}
