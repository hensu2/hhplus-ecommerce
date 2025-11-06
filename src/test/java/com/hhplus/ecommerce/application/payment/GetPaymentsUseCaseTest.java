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

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPaymentsUseCaseTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private GetPaymentsUseCase getPaymentsUseCase;

    private long userId;
    private PaymentEntity payment1;
    private PaymentEntity payment2;

    @BeforeEach
    void setUp() {
        userId = 1L;
        long now = System.currentTimeMillis();

        payment1 = new PaymentEntity(
            1L,
            1L,
            userId,
            30000,
            PaymentStatus.COMPLETED,
            now,
            now
        );

        payment2 = new PaymentEntity(
            2L,
            2L,
            userId,
            50000,
            PaymentStatus.COMPLETED,
            now,
            now
        );
    }

    @Test
    @DisplayName("사용자의 결제 목록을 조회한다")
    void getPayments() {
        // given
        when(paymentRepository.findByUserId(userId))
            .thenReturn(Arrays.asList(payment1, payment2));

        // when
        List<PaymentResponse> result = getPaymentsUseCase.execute(userId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPaymentId()).isEqualTo(1L);
        assertThat(result.get(0).getAmount()).isEqualTo(30000);
        assertThat(result.get(1).getPaymentId()).isEqualTo(2L);
        assertThat(result.get(1).getAmount()).isEqualTo(50000);
    }

    @Test
    @DisplayName("결제 내역이 없으면 빈 리스트를 반환한다")
    void getPaymentsWithNoHistory() {
        // given
        when(paymentRepository.findByUserId(userId)).thenReturn(List.of());

        // when
        List<PaymentResponse> result = getPaymentsUseCase.execute(userId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("결제 응답에 모든 필드가 올바르게 매핑된다")
    void getPaymentsResponseFields() {
        // given
        when(paymentRepository.findByUserId(userId))
            .thenReturn(Arrays.asList(payment1));

        // when
        List<PaymentResponse> result = getPaymentsUseCase.execute(userId);

        // then
        assertThat(result).hasSize(1);
        PaymentResponse response = result.get(0);
        assertThat(response.getPaymentId()).isEqualTo(1L);
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getAmount()).isEqualTo(30000);
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getCreatedAt()).isNotNull();
    }
}
