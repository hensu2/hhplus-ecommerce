package com.hhplus.ecommerce.application.payment;

import com.hhplus.ecommerce.domain.payment.PaymentEntity;
import com.hhplus.ecommerce.domain.payment.PaymentStatus;
import com.hhplus.ecommerce.infrastructure.payment.PaymentRepository;
import com.hhplus.ecommerce.presentation.payment.req.ProcessPaymentRequest;
import com.hhplus.ecommerce.presentation.payment.res.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentUseCaseTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private ProcessPaymentUseCase processPaymentUseCase;

    private long userId;
    private long orderId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        orderId = 1L;
    }

    @Test
    @DisplayName("결제 처리에 성공한다")
    void processPayment() {
        // given
        ProcessPaymentRequest request = new ProcessPaymentRequest(orderId, userId, 30000);

        long now = System.currentTimeMillis();
        PaymentEntity savedPayment = new PaymentEntity(
            1L,
            orderId,
            userId,
            30000,
            PaymentStatus.COMPLETED,
            now,
            now
        );
        when(paymentRepository.save(any(PaymentEntity.class))).thenReturn(savedPayment);

        // when
        PaymentResponse response = processPaymentUseCase.execute(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getPaymentId()).isEqualTo(1L);
        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getAmount()).isEqualTo(30000);
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("결제 금액이 0 이하일 때 예외를 발생시킨다")
    void processPaymentWithInvalidAmount() {
        // given
        ProcessPaymentRequest request = new ProcessPaymentRequest(orderId, userId, 0);

        // when & then
        assertThatThrownBy(() -> processPaymentUseCase.execute(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("결제 금액이 유효하지 않습니다.");
    }

    @Test
    @DisplayName("결제 금액이 null일 때 예외를 발생시킨다")
    void processPaymentWithNullAmount() {
        // given
        ProcessPaymentRequest request = new ProcessPaymentRequest(orderId, userId, null);

        // when & then
        assertThatThrownBy(() -> processPaymentUseCase.execute(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("결제 금액이 유효하지 않습니다.");
    }

    @Test
    @DisplayName("결제 금액이 음수일 때 예외를 발생시킨다")
    void processPaymentWithNegativeAmount() {
        // given
        ProcessPaymentRequest request = new ProcessPaymentRequest(orderId, userId, -1000);

        // when & then
        assertThatThrownBy(() -> processPaymentUseCase.execute(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("결제 금액이 유효하지 않습니다.");
    }

    @Test
    @DisplayName("결제 응답에 모든 필드가 올바르게 매핑된다")
    void processPaymentResponseFields() {
        // given
        ProcessPaymentRequest request = new ProcessPaymentRequest(orderId, userId, 50000);

        long now = System.currentTimeMillis();
        PaymentEntity savedPayment = new PaymentEntity(
            2L,
            orderId,
            userId,
            50000,
            PaymentStatus.COMPLETED,
            now,
            now
        );
        when(paymentRepository.save(any(PaymentEntity.class))).thenReturn(savedPayment);

        // when
        PaymentResponse response = processPaymentUseCase.execute(request);

        // then
        assertThat(response.getPaymentId()).isEqualTo(2L);
        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getAmount()).isEqualTo(50000);
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getCreatedAt()).isNotNull();
    }
}
