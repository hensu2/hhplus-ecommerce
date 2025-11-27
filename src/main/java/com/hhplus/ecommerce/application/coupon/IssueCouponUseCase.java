package com.hhplus.ecommerce.application.coupon;

import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.CouponHistoryEntity;
import com.hhplus.ecommerce.domain.coupon.CouponStatus;
import com.hhplus.ecommerce.infrastructure.coupon.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class IssueCouponUseCase {

    private final CouponRepository couponRepository;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;

    public CouponHistoryEntity execute(long userId, long couponId) {
        String lockKey = "coupon:stock:lock:" + couponId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 1. 락 획득 (Pub/Sub 방식 - Redisson 기본)
            // 쿠폰 발급은 짧은 대기 시간 설정 (빠른 실패)
            boolean acquired = lock.tryLock(5, 2, TimeUnit.SECONDS);

            if (!acquired) {
                throw new RuntimeException("쿠폰 발급이 지연되고 있습니다. 잠시 후 재시도해주세요.");
            }

            // 2. 트랜잭션 내에서 비즈니스 로직 수행
            return transactionTemplate.execute(status -> {
                // 쿠폰 재고 확인
                CouponEntity coupon = couponRepository.getOrThrow(couponId);

                if (coupon.getStock() <= 0) {
                    throw new IllegalStateException("쿠폰이 모두 소진되었습니다.");
                }

                // 중복 발급 체크
                couponRepository.findHistoryByUserIdAndCouponId(userId, couponId)
                        .ifPresent(history -> {
                            throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
                        });

                // 재고 감소
                CouponEntity updatedCoupon = coupon.decreaseStock();
                couponRepository.save(updatedCoupon);

                // 발급 내역 생성
                long now = System.currentTimeMillis();
                CouponHistoryEntity history = new CouponHistoryEntity(
                        0L,
                        userId,
                        couponId,
                        CouponStatus.ISSUED,
                        now,
                        null
                );
                return couponRepository.saveHistory(history);
            });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("쿠폰 발급 중 오류가 발생했습니다.", e);
        } finally {
            // 3. 락 해제 (커밋 후)
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
