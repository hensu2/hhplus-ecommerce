package com.hhplus.ecommerce.application.payment;

import com.hhplus.ecommerce.domain.payment.PaymentEntity;
import com.hhplus.ecommerce.domain.payment.PaymentStatus;
import com.hhplus.ecommerce.infrastructure.payment.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelPaymentUseCaseTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private RLock lock;

    @InjectMocks
    private CancelPaymentUseCase cancelPaymentUseCase;

    private PaymentEntity completedPayment;
    private long paymentId;

    @BeforeEach
    void setUp() {
        paymentId = 1L;
        long now = System.currentTimeMillis();

        completedPayment = new PaymentEntity(
            paymentId,
            1L,
            1L,
            30000,
            PaymentStatus.COMPLETED,
            now,
            now
        );
    }

    @Test
    @DisplayName("결제 취소에 성공한다")
    void cancelPayment() throws Exception {
        // given
        long now = System.currentTimeMillis();
        PaymentEntity cancelledPayment = new PaymentEntity(
            paymentId,
            completedPayment.getOrderId(),
            completedPayment.getUserId(),
            completedPayment.getAmount(),
            PaymentStatus.CANCELLED,
            completedPayment.getCreatedAt(),
            now
        );

        // Mock Redisson lock
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        // Mock TransactionTemplate to execute the callback
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        when(paymentRepository.getOrThrow(paymentId)).thenReturn(completedPayment);
        when(paymentRepository.save(any(PaymentEntity.class))).thenReturn(cancelledPayment);

        // when
        PaymentEntity result = cancelPaymentUseCase.execute(paymentId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(paymentId);
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    @DisplayName("존재하지 않는 결제 ID로 취소 시도 시 예외를 발생시킨다")
    void cancelPaymentWithInvalidId() throws Exception {
        // given
        // Mock Redisson lock
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        // Mock TransactionTemplate to execute the callback
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        when(paymentRepository.getOrThrow(999L))
            .thenThrow(new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));

        // when & then
        assertThatThrownBy(() -> cancelPaymentUseCase.execute(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("결제 정보를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("이미 취소된 결제를 다시 취소 시도 시 예외를 발생시킨다")
    void cancelAlreadyCancelledPayment() throws Exception {
        // given
        long now = System.currentTimeMillis();
        PaymentEntity cancelledPayment = new PaymentEntity(
            paymentId,
            1L,
            1L,
            30000,
            PaymentStatus.CANCELLED,
            now,
            now
        );

        // Mock Redisson lock
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        // Mock TransactionTemplate to execute the callback
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        when(paymentRepository.getOrThrow(paymentId)).thenReturn(cancelledPayment);

        // when & then
        assertThatThrownBy(() -> cancelPaymentUseCase.execute(paymentId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("이미 취소된 결제입니다.");
    }

    @Test
    @DisplayName("대기중인 결제는 취소할 수 없다")
    void cancelPendingPayment() throws Exception {
        // given
        long now = System.currentTimeMillis();
        PaymentEntity pendingPayment = new PaymentEntity(
            paymentId,
            1L,
            1L,
            30000,
            PaymentStatus.PENDING,
            now,
            now
        );

        // Mock Redisson lock
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        // Mock TransactionTemplate to execute the callback
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        when(paymentRepository.getOrThrow(paymentId)).thenReturn(pendingPayment);

        // when & then
        assertThatThrownBy(() -> cancelPaymentUseCase.execute(paymentId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("완료된 결제만 취소할 수 있습니다.");
    }

    @Test
    @DisplayName("실패한 결제는 취소할 수 없다")
    void cancelFailedPayment() throws Exception {
        // given
        long now = System.currentTimeMillis();
        PaymentEntity failedPayment = new PaymentEntity(
            paymentId,
            1L,
            1L,
            30000,
            PaymentStatus.FAILED,
            now,
            now
        );

        // Mock Redisson lock
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        // Mock TransactionTemplate to execute the callback
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        when(paymentRepository.getOrThrow(paymentId)).thenReturn(failedPayment);

        // when & then
        assertThatThrownBy(() -> cancelPaymentUseCase.execute(paymentId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("완료된 결제만 취소할 수 있습니다.");
    }
}
