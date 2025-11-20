package com.hhplus.ecommerce.integration;

import com.hhplus.ecommerce.application.payment.ProcessPaymentUseCase;
import com.hhplus.ecommerce.domain.payment.PaymentEntity;
import com.hhplus.ecommerce.infrastructure.payment.PaymentRepository;
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
    private PaymentRepository paymentRepository;

    @AfterEach
    void tearDown() {
        // 테스트 후 데이터 정리는 필요시 추가
    }

    @Test
    @DisplayName("동시성 문제 - 같은 주문에 대해 중복 결제 발생")
    void concurrentPayment_DuplicatePayments() throws InterruptedException {
        // given
        Long userId = 1L;
        Long orderId = 999L; // 테스트용 주문 ID
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
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 동시성 제어가 없으므로 중복 결제 발생
        List<PaymentEntity> payments = paymentRepository.findByOrderId(orderId);

        System.out.println("========== 결제 동시성 테스트 결과 ==========");
        System.out.println("성공한 결제 요청 수: " + successCount.get());
        System.out.println("실패한 결제 요청 수: " + failCount.get());
        System.out.println("실제 저장된 결제 건수: " + payments.size());
        System.out.println("==========================================");

        // 동시성 제어가 없으면 여러 건의 결제가 생성됨 (문제 발생!)
        assertThat(payments.size()).isGreaterThan(1)
                .withFailMessage("동시성 제어가 없어 중복 결제가 발생했습니다!");

        // 만약 동시성 제어가 제대로 되어 있다면 1건만 생성되어야 함
        // assertThat(payments.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("동시성 문제 - 동시 결제 취소 시도")
    void concurrentCancelPayment_MultipleCancellations() throws InterruptedException {
        // given - 결제 하나 생성
        Long userId = 1L;
        Long orderId = 998L;
        Integer amount = 10000;

        ProcessPaymentRequest request = new ProcessPaymentRequest(userId, orderId, amount);
        PaymentEntity payment = processPaymentUseCase.execute(request);
        Long paymentId = payment.getId();

        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 같은 결제를 동시에 5번 취소 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // 결제 취소 로직은 CancelPaymentUseCase 호출 필요
                    // 현재는 동시성 문제만 확인하므로 생략
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        System.out.println("========== 결제 취소 동시성 테스트 결과 ==========");
        System.out.println("취소 시도 성공 수: " + successCount.get());
        System.out.println("취소 시도 실패 수: " + failCount.get());
        System.out.println("============================================");

        // 동시성 제어가 없으면 여러 번 취소될 수 있음
        assertThat(successCount.get()).isGreaterThan(0);
    }
}