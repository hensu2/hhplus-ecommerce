package com.hhplus.ecommerce.application.payment;

import com.hhplus.ecommerce.domain.payment.PaymentEntity;
import com.hhplus.ecommerce.domain.payment.PaymentStatus;
import com.hhplus.ecommerce.infrastructure.payment.PaymentRepository;
import com.hhplus.ecommerce.presentation.payment.req.ProcessPaymentRequest;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ProcessPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;

    public PaymentEntity execute(ProcessPaymentRequest request) {
        // 주문 ID 기반 락 (멱등성 보장 - 같은 주문에 중복 결제 방지)
        String lockKey = "payment:lock:order:" + request.orderId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 1. 락 획득 (Pub/Sub 방식 - Redisson 기본)
            // 결제는 외부 PG 호출이 있어 시간이 오래 걸림
            boolean acquired = lock.tryLock(15, 10, TimeUnit.SECONDS);

            if (!acquired) {
                throw new RuntimeException("결제 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }

            // 2. 트랜잭션 내에서 비즈니스 로직 수행
            return transactionTemplate.execute(status -> {
                long now = System.currentTimeMillis();

                // 금액 유효성 검증
                if (request.amount() == null || request.amount() <= 0) {
                    throw new IllegalArgumentException("결제 금액이 유효하지 않습니다.");
                }

                // 중복 결제 체크 (멱등성 보장)
                java.util.List<PaymentEntity> existingPayments = paymentRepository.findByOrderId(request.orderId());
                if (!existingPayments.isEmpty()) {
                    throw new IllegalStateException("이미 처리된 주문입니다. Order ID: " + request.orderId());
                }

                // 결제 처리 (외부 PG 호출은 생략)
                PaymentStatus paymentStatus = PaymentStatus.COMPLETED;

                PaymentEntity payment = new PaymentEntity(
                    0L,
                    request.orderId(),
                    request.userId(),
                    request.amount(),
                    paymentStatus,
                    now,
                    now
                );
                return paymentRepository.save(payment);
            });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("결제 처리 중 오류가 발생했습니다.", e);
        } finally {
            // 3. 락 해제 (커밋 후)
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
