package com.hhplus.ecommerce.application.order;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.domain.productOption.StockUpdateType;
import com.hhplus.ecommerce.infrastructure.order.OrderRepository;
import com.hhplus.ecommerce.infrastructure.product.ProductRepository;
import com.hhplus.ecommerce.infrastructure.productOption.ProductOptionRepository;
import com.hhplus.ecommerce.presentation.order.req.CreateOrderRequest;
import com.hhplus.ecommerce.presentation.order.req.OrderItemRequest;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductRepository productRepository;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;

    public OrderEntity execute(CreateOrderRequest request) {
        List<RLock> locks = new ArrayList<>();

        // 1. 재고 락 수집 (각 상품 옵션별)
        for (OrderItemRequest item : request.items()) {
            String stockLockKey = "stock:lock:product_option:" + item.productOptionId();
            locks.add(redissonClient.getLock(stockLockKey));
        }

        // 2. 포인트 락 (포인트 사용 시 - 향후 확장)
        // if (request.getPointAmount() != null && request.getPointAmount() > 0) {
        //     String pointLockKey = "user:point:lock:" + request.userId();
        //     locks.add(redissonClient.getLock(pointLockKey));
        // }

        // 3. 쿠폰 락 (쿠폰 사용 시)
        if (request.couponHistoryId() != null) {
            // 쿠폰 히스토리 ID를 기반으로 락 (실제로는 쿠폰 ID를 조회해야 할 수 있음)
            // 현재는 couponHistoryId가 있으면 쿠폰 사용으로 간주
            // String couponLockKey = "coupon:stock:lock:" + couponId;
            // locks.add(redissonClient.getLock(couponLockKey));
        }

        // MultiLock: 모든 락을 원자적으로 획득 (Pub/Sub 기반)
        RLock multiLock = redissonClient.getMultiLock(locks.toArray(new RLock[0]));

        try {
            // 1. 락 획득 (트랜잭션 외부)
            boolean acquired = multiLock.tryLock(10, 5, TimeUnit.SECONDS);

            if (!acquired) {
                throw new RuntimeException("주문 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }

            // 2. 트랜잭션 내에서 비즈니스 로직 수행
            return transactionTemplate.execute(status -> {
                long now = System.currentTimeMillis();

                List<OrderItemEntity> orderItems = new ArrayList<>();
                int totalAmount = 0;

                // 재고 확인 및 차감
                for (OrderItemRequest item : request.items()) {
                    ProductOptionEntity option = productOptionRepository.getOrThrow(item.productOptionId());
                    ProductEntity product = productRepository.getOrThrow(option.getProductId());

                    // 재고 확인
                    if (option.getStock() < item.quantity()) {
                        throw new IllegalStateException(
                            "재고가 부족합니다. 상품: " + product.getProductName() +
                            ", 옵션: " + option.getOptionType() +
                            ", 현재 재고: " + option.getStock()
                        );
                    }

                    // 재고 감소
                    ProductOptionEntity updatedOption = option.updateStock(StockUpdateType.DECREASE, item.quantity());
                    productOptionRepository.save(updatedOption);

                    // 상품 기본 가격 + 옵션 추가 가격
                    int itemPrice = (product.getPrice().intValue() + option.getAdditionalPrice().intValue()) * item.quantity();
                    totalAmount += itemPrice;

                    orderItems.add(new OrderItemEntity(
                        0L,
                        0L,
                        option.getProductId(),
                        option.getId(),
                        product.getProductName(),
                        option.getOptionType(),
                        item.quantity(),
                        itemPrice,
                        now
                    ));
                }

                int discountAmount = 0;
                int finalAmount = totalAmount - discountAmount;

                OrderEntity order = new OrderEntity(
                    0L,
                    request.userId(),
                    totalAmount,
                    discountAmount,
                    finalAmount,
                    request.couponHistoryId(),
                    OrderStatus.PENDING,
                    now,
                    now,
                    now
                );
                OrderEntity savedOrder = orderRepository.save(order);

                for (OrderItemEntity orderItem : orderItems) {
                    OrderItemEntity itemWithOrderId = new OrderItemEntity(
                        orderItem.getId(),
                        savedOrder.getId(),
                        orderItem.getProductId(),
                        orderItem.getProductOptionId(),
                        orderItem.getProductName(),
                        orderItem.getOptionType(),
                        orderItem.getQuantity(),
                        orderItem.getPrice(),
                        orderItem.getCreatedAt()
                    );
                    orderRepository.saveItem(itemWithOrderId);
                }

                return savedOrder;
            });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("주문 처리 중 오류가 발생했습니다.", e);
        } finally {
            // 3. 트랜잭션 커밋 후 락 해제
            if (multiLock.isHeldByCurrentThread()) {
                multiLock.unlock();
            }
        }
    }
}
