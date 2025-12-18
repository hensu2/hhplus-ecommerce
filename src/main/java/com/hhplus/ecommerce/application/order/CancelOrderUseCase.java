package com.hhplus.ecommerce.application.order;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.domain.order.event.OrderCancelledEvent;
import com.hhplus.ecommerce.domain.order.event.kafka.OrderCancelledKafkaEvent;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.domain.productOption.StockUpdateType;
import com.hhplus.ecommerce.domain.productOption.event.kafka.StockChangedKafkaEvent;
import com.hhplus.ecommerce.domain.productOption.event.kafka.StockEventType;
import com.hhplus.ecommerce.infrastructure.kafka.producer.OrderKafkaProducer;
import com.hhplus.ecommerce.infrastructure.kafka.producer.StockKafkaProducer;
import com.hhplus.ecommerce.infrastructure.order.OrderRepository;
import com.hhplus.ecommerce.infrastructure.product.ProductRepository;
import com.hhplus.ecommerce.infrastructure.productOption.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CancelOrderUseCase {

    private final OrderRepository orderRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductRepository productRepository;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderKafkaProducer orderKafkaProducer;
    private final StockKafkaProducer stockKafkaProducer;

    public OrderEntity execute(long orderId) {
        // 주문 조회
        OrderEntity order = orderRepository.getOrThrow(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 주문입니다.");
        }

        // 주문 아이템 조회
        List<OrderItemEntity> orderItems = orderRepository.findItemsByOrderId(orderId);

        // 재고 락 수집 (각 상품 옵션별)
        List<RLock> locks = new ArrayList<>();
        for (OrderItemEntity item : orderItems) {
            String stockLockKey = "stock:lock:product_option:" + item.getProductOptionId();
            locks.add(redissonClient.getLock(stockLockKey));
        }

        // MultiLock: 모든 락을 원자적으로 획득
        RLock multiLock = redissonClient.getMultiLock(locks.toArray(new RLock[0]));
        boolean lockAcquired = false;

        try {
            // 락 획득 (트랜잭션 외부)
            lockAcquired = multiLock.tryLock(10, 5, TimeUnit.SECONDS);

            if (!lockAcquired) {
                throw new RuntimeException("주문 취소 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }

            // 트랜잭션 내에서 비즈니스 로직 수행
            return transactionTemplate.execute(status -> {
                long now = System.currentTimeMillis();

                // 주문 상태를 CANCELLED로 변경
                OrderEntity cancelledOrder = new OrderEntity(
                    order.getId(),
                    order.getUserId(),
                    order.getTotalAmount(),
                    order.getDiscountAmount(),
                    order.getFinalAmount(),
                    order.getCouponHistoryId(),
                    OrderStatus.CANCELLED,
                    order.getOrderedAt(),
                    order.getCreatedAt(),
                    now
                );
                OrderEntity savedOrder = orderRepository.save(cancelledOrder);

                // 재고 복구
                for (OrderItemEntity item : orderItems) {
                    ProductOptionEntity option = productOptionRepository.getOrThrow(item.getProductOptionId());
                    ProductEntity product = productRepository.getOrThrow(option.getProductId());

                    Long previousStock = option.getStock();
                    ProductOptionEntity updatedOption = option.updateStock(StockUpdateType.INCREASE, item.getQuantity());
                    ProductOptionEntity savedOption = productOptionRepository.save(updatedOption);

                    // Stock Kafka 이벤트 발행 (주문 취소로 인한 재고 증가)
                    try {
                        StockChangedKafkaEvent stockEvent = new StockChangedKafkaEvent(
                            StockEventType.STOCK_INCREASED,
                            savedOption,
                            product.getProductName(),
                            previousStock,
                            savedOption.getStock(),
                            item.getQuantity(),
                            "ORDER_CANCELLED"
                        );
                        stockKafkaProducer.publish(stockEvent);
                    } catch (Exception e) {
                        // Kafka 발행 실패는 로깅만 하고 주문 취소 처리는 계속 진행
                    }
                }

                // 이벤트 발행 (트랜잭션 커밋 후 비동기 실행)
                eventPublisher.publishEvent(new OrderCancelledEvent(
                    savedOrder.getId(),
                    orderItems,
                    savedOrder.getUpdatedAt()
                ));

                // Kafka 이벤트 발행 (Dual Write Pattern)
                try {
                    OrderCancelledKafkaEvent kafkaEvent = new OrderCancelledKafkaEvent(savedOrder, orderItems);
                    orderKafkaProducer.publish(kafkaEvent);
                } catch (Exception e) {
                    // Kafka 발행 실패는 로깅만 하고 주문 취소 처리는 계속 진행
                    // (기존 ApplicationEventPublisher는 여전히 동작)
                }

                return savedOrder;
            });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("주문 취소 처리 중 오류가 발생했습니다.", e);
        } finally {
            // 트랜잭션 커밋 후 락 해제
            if (lockAcquired) {
                try {
                    multiLock.unlock();
                } catch (IllegalMonitorStateException e) {
                    // 락이 이미 해제된 경우 무시
                }
            }
        }
    }
}
