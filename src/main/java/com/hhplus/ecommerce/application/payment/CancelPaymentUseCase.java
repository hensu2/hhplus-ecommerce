package com.hhplus.ecommerce.application.payment;

import com.hhplus.ecommerce.domain.payment.PaymentEntity;
import com.hhplus.ecommerce.domain.payment.PaymentStatus;
import com.hhplus.ecommerce.infrastructure.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CancelPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;

    public PaymentEntity execute(long paymentId) {
        // 결제 ID 기반 락 (중복 취소 방지)
        String lockKey = "payment:lock:" + paymentId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 1. 락 획득 (Pub/Sub 방식 - Redisson 기본)
            // 결제 취소도 외부 PG 호출이 있어 시간이 오래 걸림
            boolean acquired = lock.tryLock(15, 10, TimeUnit.SECONDS);

            if (!acquired) {
                throw new RuntimeException("결제 취소 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }

            // 2. 트랜잭션 내에서 비즈니스 로직 수행
            return transactionTemplate.execute(status -> {
                PaymentEntity payment = paymentRepository.getOrThrow(paymentId);

                // 상태 검증
                if (payment.getStatus() == PaymentStatus.CANCELLED) {
                    throw new IllegalStateException("이미 취소된 결제입니다.");
                }

                if (payment.getStatus() != PaymentStatus.COMPLETED) {
                    throw new IllegalStateException("완료된 결제만 취소할 수 있습니다.");
                }

                // 결제 취소 처리 (외부 PG 호출은 생략)
                long now = System.currentTimeMillis();
                PaymentEntity cancelledPayment = new PaymentEntity(
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getUserId(),
                    payment.getAmount(),
                    PaymentStatus.CANCELLED,
                    payment.getCreatedAt(),
                    now
                );
                return paymentRepository.save(cancelledPayment);
            });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("결제 취소 처리 중 오류가 발생했습니다.", e);
        } finally {
            // 3. 락 해제 (커밋 후)
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
