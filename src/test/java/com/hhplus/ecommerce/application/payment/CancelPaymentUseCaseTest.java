package com.hhplus.ecommerce.application.payment;

import com.hhplus.ecommerce.domain.payment.PaymentEntity;
import com.hhplus.ecommerce.domain.payment.PaymentStatus;
import com.hhplus.ecommerce.infrastructure.payment.PaymentRepository;
import com.hhplus.ecommerce.presentation.payment.res.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelPaymentUseCaseTest {

    @Mock
    private PaymentRepository paymentRepository;

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
    void cancelPayment() {
        // given
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(completedPayment));

        long now = System.currentTimeMillis();
        PaymentEntity cancelledPayment = new PaymentEntity(
            paymentId,
            completedPayment.orderId(),
            completedPayment.userId(),
            completedPayment.amount(),
            PaymentStatus.CANCELLED,
            completedPayment.createdAt(),
            now
        );
        when(paymentRepository.save(any(PaymentEntity.class))).thenReturn(cancelledPayment);

        // when
        PaymentResponse response = cancelPaymentUseCase.execute(paymentId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getPaymentId()).isEqualTo(paymentId);
        assertThat(response.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("존재하지 않는 결제 ID로 취소 시도 시 예외를 발생시킨다")
    void cancelPaymentWithInvalidId() {
        // given
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> cancelPaymentUseCase.execute(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("결제 정보를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("이미 취소된 결제를 다시 취소 시도 시 예외를 발생시킨다")
    void cancelAlreadyCancelledPayment() {
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
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(cancelledPayment));

        // when & then
        assertThatThrownBy(() -> cancelPaymentUseCase.execute(paymentId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("이미 취소된 결제입니다.");
    }

    @Test
    @DisplayName("대기중인 결제는 취소할 수 없다")
    void cancelPendingPayment() {
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
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(pendingPayment));

        // when & then
        assertThatThrownBy(() -> cancelPaymentUseCase.execute(paymentId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("완료된 결제만 취소할 수 있습니다.");
    }

    @Test
    @DisplayName("실패한 결제는 취소할 수 없다")
    void cancelFailedPayment() {
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
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(failedPayment));

        // when & then
        assertThatThrownBy(() -> cancelPaymentUseCase.execute(paymentId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("완료된 결제만 취소할 수 있습니다.");
    }
}
