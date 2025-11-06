package com.hhplus.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.presentation.order.req.CreateOrderRequest;
import com.hhplus.ecommerce.presentation.order.req.OrderItemRequest;
import com.hhplus.ecommerce.presentation.payment.req.ProcessPaymentRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("PaymentController 통합 테스트")
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("결제 처리 성공")
    void processPayment_Success() throws Exception {
        // given - 주문 생성
        Long orderId = createOrder(1L, Arrays.asList(new OrderItemRequest(1L, 1)), null);

        ProcessPaymentRequest request = new ProcessPaymentRequest(orderId, 1L, 50000);

        // when & then
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.amount").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.paymentMethod").exists())
                .andExpect(jsonPath("$.paidAt").exists());
    }

    @Test
    @DisplayName("결제 처리 실패 - 존재하지 않는 주문")
    void processPayment_OrderNotFound_Fail() throws Exception {
        // given
        Long nonExistentOrderId = 99999L;
        ProcessPaymentRequest request = new ProcessPaymentRequest(nonExistentOrderId, 1L, 50000);

        // when & then
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("결제 처리 실패 - 포인트 부족")
    void processPayment_InsufficientPoints_Fail() throws Exception {
        // given - 주문 생성
        Long orderId = createOrder(1L, Arrays.asList(new OrderItemRequest(1L, 1)), null);

        ProcessPaymentRequest request = new ProcessPaymentRequest(orderId, 1L, 10000000);

        // when & then
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("결제 목록 조회 성공")
    void getPayments_Success() throws Exception {
        // given
        Long userId = 1L;

        // when & then
        mockMvc.perform(get("/api/payments")
                        .param("userId", userId.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payments").isArray());
    }

    @Test
    @DisplayName("결제 취소 성공")
    void cancelPayment_Success() throws Exception {
        // given - 주문 생성 및 결제
        Long orderId = createOrder(1L, Arrays.asList(new OrderItemRequest(1L, 1)), null);
        ProcessPaymentRequest paymentRequest = new ProcessPaymentRequest(orderId, 1L, 50000);

        String paymentResponse = mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long paymentId = objectMapper.readTree(paymentResponse).get("paymentId").asLong();

        // when & then - 결제 취소
        mockMvc.perform(post("/api/payments/{paymentId}/cancel", paymentId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("결제 취소 실패 - 존재하지 않는 결제")
    void cancelPayment_NotFound_Fail() throws Exception {
        // given
        Long nonExistentPaymentId = 99999L;

        // when & then
        mockMvc.perform(post("/api/payments/{paymentId}/cancel", nonExistentPaymentId))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("결제 처리 후 목록 조회 확인")
    void processPaymentAndGetList_Success() throws Exception {
        // given - 주문 생성 및 결제
        Long orderId = createOrder(2L, Arrays.asList(new OrderItemRequest(1L, 1)), null);
        ProcessPaymentRequest request = new ProcessPaymentRequest(orderId, 2L, 50000);

        String paymentResponse = mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long paymentId = objectMapper.readTree(paymentResponse).get("paymentId").asLong();

        // when & then - 결제 목록 조회
        mockMvc.perform(get("/api/payments")
                        .param("userId", "2"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payments[?(@.paymentId == " + paymentId + ")]").exists());
    }

    @Test
    @DisplayName("결제 처리 후 취소 플로우")
    void processAndCancelPayment_Success() throws Exception {
        // given - 주문 생성 및 결제
        Long orderId = createOrder(1L, Arrays.asList(new OrderItemRequest(1L, 1)), null);
        ProcessPaymentRequest paymentRequest = new ProcessPaymentRequest(orderId, 1L, 50000);

        String paymentResponse = mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long paymentId = objectMapper.readTree(paymentResponse).get("paymentId").asLong();

        // when - 결제 취소
        mockMvc.perform(post("/api/payments/{paymentId}/cancel", paymentId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("결제 목록 조회 - 결제 구조 검증")
    void getPayments_Structure_Success() throws Exception {
        // given - 결제 생성
        Long orderId = createOrder(3L, Arrays.asList(new OrderItemRequest(1L, 1)), null);
        ProcessPaymentRequest request = new ProcessPaymentRequest(orderId, 3L, 50000);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // when
        String response = mockMvc.perform(get("/api/payments")
                        .param("userId", "3"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // then - 결제가 있는 경우 구조 검증
        var payments = objectMapper.readTree(response).get("payments");
        if (payments.size() > 0) {
            mockMvc.perform(get("/api/payments")
                            .param("userId", "3"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payments[0].paymentId").exists())
                    .andExpect(jsonPath("$.payments[0].orderId").exists())
                    .andExpect(jsonPath("$.payments[0].userId").exists())
                    .andExpect(jsonPath("$.payments[0].amount").exists())
                    .andExpect(jsonPath("$.payments[0].status").exists())
                    .andExpect(jsonPath("$.payments[0].paymentMethod").exists())
                    .andExpect(jsonPath("$.payments[0].paidAt").exists());
        }
    }

    @Test
    @DisplayName("여러 건 결제 후 목록 조회")
    void multiplePaymentsAndGetList_Success() throws Exception {
        // given - 여러 주문 생성 및 결제
        Long userId = 4L;

        Long orderId1 = createOrder(userId, Arrays.asList(new OrderItemRequest(1L, 1)), null);
        Long orderId2 = createOrder(userId, Arrays.asList(new OrderItemRequest(2L, 1)), null);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProcessPaymentRequest(orderId1, userId, 50000))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProcessPaymentRequest(orderId2, userId, 30000))))
                .andExpect(status().isCreated());

        // when & then - 결제 목록 조회
        mockMvc.perform(get("/api/payments")
                        .param("userId", userId.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payments", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("결제 후 주문 완료 상태 변경 확인")
    void processPaymentAndCheckOrderStatus_Success() throws Exception {
        // given - 주문 생성
        Long orderId = createOrder(1L, Arrays.asList(new OrderItemRequest(1L, 1)), null);

        // when - 결제 처리
        ProcessPaymentRequest request = new ProcessPaymentRequest(orderId, 1L, 50000);
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // then - 주문 완료 처리 (결제 후 주문 완료)
        mockMvc.perform(post("/api/orders/{orderId}/complete", orderId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("결제 금액과 주문 금액 일치 확인")
    void processPayment_AmountMatch_Success() throws Exception {
        // given - 주문 생성
        List<OrderItemRequest> items = Arrays.asList(new OrderItemRequest(1L, 1));
        CreateOrderRequest createRequest = new CreateOrderRequest(1L, items, null);

        String orderResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long orderId = objectMapper.readTree(orderResponse).get("orderId").asLong();
        int orderAmount = objectMapper.readTree(orderResponse).get("finalAmount").asInt();

        // when - 결제 처리
        ProcessPaymentRequest paymentRequest = new ProcessPaymentRequest(orderId, 1L, orderAmount);
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(orderAmount));
    }

    // 헬퍼 메서드: 주문 생성
    private Long createOrder(Long userId, List<OrderItemRequest> items, Long couponHistoryId) throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(userId, items, couponHistoryId);

        String response = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("orderId").asLong();
    }
}
