package com.hhplus.ecommerce.application.productOption;

import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.domain.productOption.StockUpdateType;
import com.hhplus.ecommerce.domain.productOption.event.kafka.StockChangedKafkaEvent;
import com.hhplus.ecommerce.domain.productOption.event.kafka.StockEventType;
import com.hhplus.ecommerce.infrastructure.kafka.producer.StockKafkaProducer;
import com.hhplus.ecommerce.infrastructure.product.ProductRepository;
import com.hhplus.ecommerce.infrastructure.productOption.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DecreaseStockUseCase {

    private final ProductOptionRepository productOptionRepository;
    private final ProductRepository productRepository;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;
    private final StockKafkaProducer stockKafkaProducer;

    public ProductOptionEntity execute(Long productOptionId, long quantity) {
        String lockKey = "stock:lock:product_option:" + productOptionId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 1. 락 획득 (Pub/Sub 방식 - Redisson 기본)
            boolean acquired = lock.tryLock(10, 3, TimeUnit.SECONDS);

            if (!acquired) {
                throw new RuntimeException("재고 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }

            // 2. 트랜잭션 내에서 비즈니스 로직 수행
            return transactionTemplate.execute(status -> {
                // 상품 옵션 조회
                ProductOptionEntity productOption = productOptionRepository.getOrThrow(productOptionId);
                Long previousStock = productOption.getStock();

                // 재고 확인
                if (productOption.getStock() < quantity) {
                    throw new IllegalStateException("재고가 부족합니다. 현재 재고: " + productOption.getStock());
                }

                // 재고 감소
                ProductOptionEntity updatedOption = productOption.updateStock(StockUpdateType.DECREASE, (int) quantity);
                ProductOptionEntity savedOption = productOptionRepository.save(updatedOption);

                // Kafka 이벤트 발행 (Dual Write Pattern)
                try {
                    ProductEntity product = productRepository.getOrThrow(productOption.getProductId());
                    StockChangedKafkaEvent event = new StockChangedKafkaEvent(
                        StockEventType.STOCK_DECREASED,
                        savedOption,
                        product.getProductName(),
                        previousStock,
                        savedOption.getStock(),
                        (int) quantity,
                        "DIRECT_DECREASE"
                    );
                    stockKafkaProducer.publish(event);
                } catch (Exception e) {
                    // Kafka 발행 실패는 로깅만 하고 재고 처리는 계속 진행
                }

                return savedOption;
            });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("재고 처리 중 오류가 발생했습니다.", e);
        } finally {
            // 3. 락 해제 (커밋 후)
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}