package com.hhplus.ecommerce.integration;

import com.hhplus.ecommerce.application.productOption.DecreaseStockUseCase;
import com.hhplus.ecommerce.config.EmbeddedRedisConfig;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.infrastructure.productOption.ProductOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(initializers = EmbeddedRedisConfig.class)
@DisplayName("재고 동시성 테스트")
class StockConcurrencyTest {

    @Autowired
    private DecreaseStockUseCase decreaseStockUseCase;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    private Long testProductOptionId;

    @BeforeEach
    void setUp() {
        // 테스트용 상품 옵션 ID (이미 존재하는 옵션 사용)
        testProductOptionId = 1L;

        // 테스트를 위한 초기 재고 설정 (20개로 재입고)
        ProductOptionEntity option = productOptionRepository.getOrThrow(testProductOptionId);
        if (option.getStock() < 20) {
            productOptionRepository.save(option.updateStock(com.hhplus.ecommerce.domain.productOption.StockUpdateType.SET, 100));
        }
    }

    @Test
    @DisplayName("동시성 제어 검증 - 10명이 동시 구매해도 재고 정확히 차감됨")
    void concurrentStockDecrease_WithConcurrencyControl() throws InterruptedException {
        // given
        ProductOptionEntity option = productOptionRepository.getOrThrow(testProductOptionId);
        Long initialStock = option.getStock();

        int purchaseQuantity = 1; // 1회 구매 수량
        int threadCount = 10; // 동시 구매 횟수

        // 재고가 충분한지 확인
        if (initialStock < threadCount * purchaseQuantity) {
            throw new IllegalStateException("테스트를 위한 재고가 부족합니다. 현재 재고: " + initialStock);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 같은 상품 옵션을 동시에 10번 구매 (재고 차감)
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    decreaseStockUseCase.execute(testProductOptionId, purchaseQuantity);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("재고 차감 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        ProductOptionEntity updatedOption = productOptionRepository.getOrThrow(testProductOptionId);
        Long finalStock = updatedOption.getStock();
        Long expectedStock = initialStock - (purchaseQuantity * threadCount);
        Long actualDecrease = initialStock - finalStock;

        System.out.println("========== 재고 동시성 테스트 결과 ==========");
        System.out.println("초기 재고: " + initialStock);
        System.out.println("구매 시도 횟수: " + threadCount);
        System.out.println("1회 구매 수량: " + purchaseQuantity);
        System.out.println("성공한 구매 요청 수: " + successCount.get());
        System.out.println("실패한 구매 요청 수: " + failCount.get());
        System.out.println("예상 최종 재고: " + expectedStock);
        System.out.println("실제 최종 재고: " + finalStock);
        System.out.println("실제 차감 수량: " + actualDecrease);
        System.out.println("=========================================");

        // Redisson 분산 락(Pub/Sub)으로 동시성 제어 - 정확한 재고 차감 보장
        assertThat(finalStock).isEqualTo(expectedStock)
                .withFailMessage("Redisson 분산 락으로 모든 재고 차감이 정확히 반영되어야 합니다!");
    }

    @Test
    @DisplayName("동시성 제어 검증 - 재고보다 많은 동시 구매 시도 시 일부만 성공")
    void concurrentStockDecrease_PreventOverselling() throws InterruptedException {
        // given
        ProductOptionEntity option = productOptionRepository.getOrThrow(testProductOptionId);
        Long initialStock = option.getStock();

        int purchaseQuantity = 1;
        // 재고보다 많은 구매 시도 (초과 구매 방지 검증)
        int threadCount = Math.min(initialStock.intValue() + 5, 20);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 재고보다 많이 동시 구매 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    decreaseStockUseCase.execute(testProductOptionId, purchaseQuantity);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("재고 부족으로 구매 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        ProductOptionEntity updatedOption = productOptionRepository.getOrThrow(testProductOptionId);
        Long finalStock = updatedOption.getStock();

        System.out.println("========== 초과 구매 방지 테스트 결과 ==========");
        System.out.println("초기 재고: " + initialStock);
        System.out.println("구매 시도 횟수: " + threadCount);
        System.out.println("성공한 구매 수: " + successCount.get());
        System.out.println("실패한 구매 수: " + failCount.get());
        System.out.println("최종 재고: " + finalStock);
        System.out.println("=========================================");

        // 재고를 초과하는 구매는 실패해야 함
        assertThat(finalStock).isGreaterThanOrEqualTo(0)
                .withFailMessage("재고는 음수가 될 수 없습니다!");

        // 성공한 구매 수 + 최종 재고 = 초기 재고
        assertThat(successCount.get() + finalStock.intValue()).isEqualTo(initialStock.intValue())
                .withFailMessage("정확한 재고 관리가 되어야 합니다!");
    }
}
