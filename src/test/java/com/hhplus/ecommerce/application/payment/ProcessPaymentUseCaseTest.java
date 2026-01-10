package com.hhplus.ecommerce.application.payment;

import com.hhplus.ecommerce.domain.payment.PaymentEntity;
import com.hhplus.ecommerce.domain.payment.PaymentStatus;
import com.hhplus.ecommerce.infrastructure.payment.PaymentRepository;
import com.hhplus.ecommerce.presentation.payment.req.ProcessPaymentRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessPaymentUseCase 단위 테스트")
class ProcessPaymentUseCaseTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private RLock lock;

    @InjectMocks
    private ProcessPaymentUseCase processPaymentUseCase;

    @Test
    @DisplayName("결제 처리 성공")
    void execute_Success() throws Exception {
        // given
        Long userId = 1L;
        Long orderId = 1L;
        Integer amount = 10000;
        ProcessPaymentRequest request = new ProcessPaymentRequest(userId, orderId, amount);

        PaymentEntity savedPayment = new PaymentEntity(1L, orderId, userId, amount, PaymentStatus.COMPLETED, 0L, 0L);

        // Mock Redisson lock
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        // Mock TransactionTemplate to execute the callback
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());
        when(paymentRepository.save(any(PaymentEntity.class))).thenReturn(savedPayment);

        // when
        PaymentEntity result = processPaymentUseCase.execute(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(paymentRepository, times(1)).save(any(PaymentEntity.class));
    }

    @Test
    @DisplayName("결제 금액이 0 이하인 경우 실패")
    void execute_InvalidAmount_Fail() throws Exception {
        // given
        ProcessPaymentRequest request = new ProcessPaymentRequest(1L, 1L, 0);

        // Mock Redisson lock
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        // Mock TransactionTemplate to execute the callback
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        // when & then
        assertThatThrownBy(() -> processPaymentUseCase.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("결제 금액이 유효하지 않습니다.");
    }
}
