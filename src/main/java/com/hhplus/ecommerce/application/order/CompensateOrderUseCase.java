package com.hhplus.ecommerce.application.order;

import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.order.OrderItemEntity;
import com.hhplus.ecommerce.domain.order.OrderStatus;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.domain.productOption.StockUpdateType;
import com.hhplus.ecommerce.infrastructure.order.OrderRepository;
import com.hhplus.ecommerce.infrastructure.productOption.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * SAGA 보상 트랜잭션 UseCase
 * 판매 랭킹 업데이트 실패 시 주문 취소 + 재고 복구를 수행
 * 이벤트를 발행하지 않아 무한 루프 방지
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompensateOrderUseCase {

    private final OrderRepository orderRepository;
    private final ProductOptionRepository productOptionRepository;
    private final RedissonClient redissonClient;

    @Transactional
    public OrderEntity execute(Long orderId, List<OrderItemEntity> orderItems) {
        log.warn("SAGA 보상 트랜잭션 시작 - orderId: {}", orderId);

        // 주문 조회
        OrderEntity order = orderRepository.getOrThrow(orderId);

        // 이미 취소된 주문이면 보상 불필요
        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.info("이미 취소된 주문 - 보상 생략 - orderId: {}", orderId);
            return order;
        }

        // 재고 락 수집
        List<RLock> locks = new ArrayList<>();
        for (OrderItemEntity item : orderItems) {
            String stockLockKey = "stock:lock:product_option:" + item.getProductOptionId();
            locks.add(redissonClient.getLock(stockLockKey));
        }

        RLock multiLock = redissonClient.getMultiLock(locks.toArray(new RLock[0]));
        boolean lockAcquired = false;

        try {
            lockAcquired = multiLock.tryLock(10, 5, TimeUnit.SECONDS);

            if (!lockAcquired) {
                throw new RuntimeException("SAGA 보상 트랜잭션 - 락 획득 실패");
            }

            long now = System.currentTimeMillis();

            // 1. 재고 복구
            for (OrderItemEntity item : orderItems) {
                ProductOptionEntity option = productOptionRepository.getOrThrow(item.getProductOptionId());
                ProductOptionEntity updatedOption = option.updateStock(StockUpdateType.INCREASE, item.getQuantity());
                productOptionRepository.save(updatedOption);
                log.info("SAGA 보상 - 재고 복구 완료 - productOptionId: {}, quantity: {}",
                    item.getProductOptionId(), item.getQuantity());
            }

            // 2. 주문 취소 (이벤트 발행 없이)
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
            log.warn("SAGA 보상 트랜잭션 완료 - orderId: {}, 주문 취소 및 재고 복구 성공", orderId);

            return savedOrder;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("SAGA 보상 트랜잭션 처리 중 오류 발생", e);
        } finally {
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