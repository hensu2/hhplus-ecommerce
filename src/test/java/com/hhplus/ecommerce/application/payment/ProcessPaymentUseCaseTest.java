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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessPaymentUseCase 단위 테스트")
class ProcessPaymentUseCaseTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private ProcessPaymentUseCase processPaymentUseCase;

    @Test
    @DisplayName("결제 처리 성공")
    void execute_Success() {
        // given
        Long userId = 1L;
        Long orderId = 1L;
        Integer amount = 10000;
        ProcessPaymentRequest request = new ProcessPaymentRequest(userId, orderId, amount);

        PaymentEntity savedPayment = new PaymentEntity(1L, orderId, userId, amount, PaymentStatus.COMPLETED, 0L, 0L);
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
    void execute_InvalidAmount_Fail() {
        // given
        ProcessPaymentRequest request = new ProcessPaymentRequest(1L, 1L, 0);

        // when & then
        assertThatThrownBy(() -> processPaymentUseCase.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("결제 금액이 유효하지 않습니다.");
    }
}
