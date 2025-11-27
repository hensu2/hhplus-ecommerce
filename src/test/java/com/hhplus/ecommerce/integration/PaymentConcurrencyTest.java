package com.hhplus.ecommerce.integration;

import com.hhplus.ecommerce.application.order.CreateOrderUseCase;
import com.hhplus.ecommerce.application.payment.CancelPaymentUseCase;
import com.hhplus.ecommerce.application.payment.ProcessPaymentUseCase;
import com.hhplus.ecommerce.domain.order.OrderEntity;
import com.hhplus.ecommerce.domain.payment.PaymentEntity;
import com.hhplus.ecommerce.domain.payment.PaymentStatus;
import com.hhplus.ecommerce.infrastructure.payment.PaymentRepository;
import com.hhplus.ecommerce.presentation.order.req.CreateOrderRequest;
import com.hhplus.ecommerce.presentation.order.req.OrderItemRequest;
import com.hhplus.ecommerce.presentation.payment.req.ProcessPaymentRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("결제 동시성 테스트")
class PaymentConcurrencyTest {

    @Autowired
    private ProcessPaymentUseCase processPaymentUseCase;

    @Autowired
    private CancelPaymentUseCase cancelPaymentUseCase;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @AfterEach
    void tearDown() {
        // 테스트 후 데이터 정리는 필요시 추가
    }

    @Test
    @DisplayName("동시성 제어 검증 - 같은 주문에 10번 결제 시도해도 1건만 생성")
    void concurrentPayment_PreventDuplicatePayments() throws InterruptedException {
        // given - 먼저 주문 생성
        Long userId = 1L;

        // 주문 생성
        CreateOrderRequest orderRequest = new CreateOrderRequest(
                userId,
                List.of(new OrderItemRequest(1L, 1)),  // 상품 옵션 ID 1, 수량 1
                null
        );
        OrderEntity order = createOrderUseCase.execute(orderRequest);
        Long orderId = order.getId();
        Integer amount = 10000;
        int threadCount = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 같은 주문에 대해 동시에 10번 결제 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    ProcessPaymentRequest request = new ProcessPaymentRequest(userId, orderId, amount);
                    processPaymentUseCase.execute(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("결제 실패: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - Redisson 분산 락으로 중복 결제 방지
        List<PaymentEntity> payments = paymentRepository.findByOrderId(orderId);

        System.out.println("========== 결제 동시성 테스트 결과 (Redisson) ==========");
        System.out.println("성공한 결제 요청 수: " + successCount.get());
        System.out.println("실패한 결제 요청 수: " + failCount.get());
        System.out.println("실제 저장된 결제 건수: " + payments.size());
        System.out.println("================================================");

        // Redisson 분산 락으로 중복 결제 방지 - 1건만 생성 (멱등성 보장)
        assertThat(payments.size()).isEqualTo(1)
                .withFailMessage("Redisson 분산 락으로 중복 결제가 방지되어야 합니다!");

        // 성공은 1건, 나머지는 실패해야 함
        assertThat(successCount.get()).isEqualTo(1)
                .withFailMessage("중복 결제 시도는 실패해야 합니다!");
        assertThat(failCount.get()).isEqualTo(threadCount - 1)
                .withFailMessage("중복 결제 시도는 모두 실패해야 합니다!");
    }

    @Test
    @DisplayName("동시성 제어 검증 - 10명이 동시 취소 시도해도 1건만 취소됨")
    void concurrentCancelPayment_PreventDuplicateCancellations() throws InterruptedException {
        // given - 주문 및 결제 생성
        Long userId = 1L;

        // 주문 생성
        CreateOrderRequest orderRequest = new CreateOrderRequest(
                userId,
                List.of(new OrderItemRequest(1L, 1)),  // 상품 옵션 ID 1, 수량 1
                null
        );
        OrderEntity order = createOrderUseCase.execute(orderRequest);
        Long orderId = order.getId();
        Integer amount = 10000;

        // 결제 생성
        ProcessPaymentRequest request = new ProcessPaymentRequest(userId, orderId, amount);
        PaymentEntity payment = processPaymentUseCase.execute(request);
        Long paymentId = payment.getId();

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 같은 결제를 동시에 10번 취소 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    cancelPaymentUseCase.execute(paymentId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("취소 실패: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - Redisson 분산 락으로 1건만 취소 성공
        PaymentEntity finalPayment = paymentRepository.getOrThrow(paymentId);

        System.out.println("========== 결제 취소 동시성 테스트 결과 (Redisson) ==========");
        System.out.println("취소 시도 성공 수: " + successCount.get());
        System.out.println("취소 시도 실패 수: " + failCount.get());
        System.out.println("최종 결제 상태: " + finalPayment.getStatus());
        System.out.println("======================================================");

        // Redisson 분산 락으로 1건만 취소 성공, 나머지는 실패
        assertThat(successCount.get()).isEqualTo(1)
                .withFailMessage("Redisson 분산 락으로 중복 취소 시도는 실패해야 합니다!");
        assertThat(failCount.get()).isEqualTo(threadCount - 1)
                .withFailMessage("중복 취소 시도는 모두 실패해야 합니다!");
        assertThat(finalPayment.getStatus()).isEqualTo(PaymentStatus.CANCELLED)
                .withFailMessage("최종 상태는 CANCELLED이어야 합니다!");
    }
}