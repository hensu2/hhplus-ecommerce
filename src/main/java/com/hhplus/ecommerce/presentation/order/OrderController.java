package com.hhplus.ecommerce.presentation.order;

import com.hhplus.ecommerce.presentation.order.req.CancelOrderRequest;
import com.hhplus.ecommerce.presentation.order.req.CreateOrderRequest;
import com.hhplus.ecommerce.presentation.order.res.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Tag(name = "주문", description = "주문 관리 API")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final AtomicLong ORDER_ID_GENERATOR = new AtomicLong(2);
    private static final Map<Long, OrderResponse> ORDERS = new LinkedHashMap<>();

    static {
        // 초기 데이터
        List<OrderItemResponse> items = List.of(
            new OrderItemResponse(1L, "노트북", 1L, "색상:블랙", 2, 890000, 1780000)
        );
        ORDERS.put(1L, new OrderResponse(
            1L, 1L, "PAID", items, 1780000, 0, 0, 1780000, "2024-10-30T00:00:00"
        ));
    }

    // 주문 생성 (POST /api/orders)
    @Operation(summary = "주문 생성", description = "장바구니의 상품들로 주문을 생성합니다. 재고 확인 및 차감, 쿠폰 적용, 포인트 차감이 트랜잭션으로 처리됩니다.")
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        Long orderId = ORDER_ID_GENERATOR.getAndIncrement();

        List<OrderItemResponse> items = List.of(
            new OrderItemResponse(1L, "상품명", 1L, "색상:블랙", 2, 10000, 20000)
        );

        int totalAmount = 50000;
        int discountAmount = request.getCouponHistoryId() != null ? 5000 : 0;
        int pointDiscount = request.getUsePoint() != null ? request.getUsePoint() : 0;
        int finalAmount = totalAmount - discountAmount - pointDiscount;

        OrderResponse order = new OrderResponse(
            orderId, 1L, "PENDING", items, totalAmount, discountAmount, pointDiscount, finalAmount,
            LocalDateTime.now().toString()
        );

        ORDERS.put(orderId, order);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    // 주문 목록 조회 (GET /api/orders)
    @Operation(summary = "주문 목록 조회", description = "사용자의 주문 목록을 조회합니다.")
    @GetMapping
    public OrderListResponse getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        List<OrderListItemResponse> content = ORDERS.values().stream()
                .filter(order -> status == null || status.equals(order.getStatus()))
                .map(order -> new OrderListItemResponse(
                    order.getOrderId(),
                    order.getStatus(),
                    order.getTotalAmount(),
                    order.getFinalAmount(),
                    order.getItems().size(),
                    order.getOrderedAt()
                ))
                .collect(Collectors.toList());

        return new OrderListResponse(content, content.size(), 1, size, page);
    }

    // 주문 상세 조회 (GET /api/orders/{orderId})
    @Operation(summary = "주문 상세 조회", description = "특정 주문의 상세 정보를 조회합니다.")
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrderDetail(@PathVariable Long orderId) {
        OrderResponse order = ORDERS.get(orderId);

        if (order == null) {
            throw new RuntimeException("주문을 찾을 수 없습니다.");
        }

        OrderDetailResponse.CouponInfo coupon = new OrderDetailResponse.CouponInfo(
            "신규 회원 10% 할인 쿠폰", order.getDiscountAmount()
        );

        OrderDetailResponse.PaymentInfo payment = null;
        if ("PAID".equals(order.getStatus())) {
            payment = new OrderDetailResponse.PaymentInfo(
                1L, order.getFinalAmount(), "2024-10-30T00:00:10"
            );
        }

        OrderDetailResponse response = new OrderDetailResponse(
            order.getOrderId(),
            order.getUserId(),
            order.getStatus(),
            order.getItems(),
            order.getTotalAmount(),
            order.getDiscountAmount(),
            order.getPointDiscount(),
            order.getFinalAmount(),
            coupon,
            payment,
            order.getOrderedAt()
        );

        return ResponseEntity.ok(response);
    }

    // 주문 취소 (POST /api/orders/{orderId}/cancel)
    @Operation(summary = "주문 취소", description = "주문을 취소합니다. 재고, 포인트, 쿠폰이 복구됩니다.")
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<CancelOrderResponse> cancelOrder(
            @PathVariable Long orderId,
            @RequestBody CancelOrderRequest request) {

        OrderResponse order = ORDERS.get(orderId);

        if (order == null) {
            throw new RuntimeException("주문을 찾을 수 없습니다.");
        }

        if ("PAID".equals(order.getStatus())) {
            throw new RuntimeException("이미 결제 완료된 주문은 취소할 수 없습니다.");
        }

        OrderResponse updatedOrder = new OrderResponse(
            order.getOrderId(), order.getUserId(), "CANCELLED", order.getItems(),
            order.getTotalAmount(), order.getDiscountAmount(), order.getPointDiscount(),
            order.getFinalAmount(), order.getOrderedAt()
        );
        ORDERS.put(orderId, updatedOrder);

        CancelOrderResponse response = new CancelOrderResponse(
            orderId, "CANCELLED", order.getFinalAmount(), order.getFinalAmount(),
            true, LocalDateTime.now().toString()
        );

        return ResponseEntity.ok(response);
    }
}
