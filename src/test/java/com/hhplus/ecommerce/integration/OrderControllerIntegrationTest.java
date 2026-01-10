package com.hhplus.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.config.EmbeddedRedisConfig;
import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.productOption.ProductOptionEntity;
import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.infrastructure.product.jpa.ProductJpaRepository;
import com.hhplus.ecommerce.infrastructure.productOption.jpa.ProductOptionJpaRepository;
import com.hhplus.ecommerce.infrastructure.user.jpa.UserJpaRepository;
import com.hhplus.ecommerce.presentation.order.req.CreateOrderRequest;
import com.hhplus.ecommerce.presentation.order.req.OrderItemRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ContextConfiguration(initializers = EmbeddedRedisConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("OrderController 통합 테스트")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private ProductOptionJpaRepository productOptionJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    private UserEntity testUser;
    private ProductEntity testProduct;
    private ProductOptionEntity testProductOption;

    @BeforeEach
    void setUp() {
        long now = System.currentTimeMillis();

        // 테스트용 유저 생성
        testUser = userJpaRepository.save(new UserEntity(
            null, "테스트유저", 1000000L, "USER", now, now
        ));

        // 테스트용 상품 생성
        testProduct = productJpaRepository.save(new ProductEntity(
            null, 1L, "테스트상품", null, 10000L, now, now
        ));

        // 테스트용 상품 옵션 생성
        testProductOption = productOptionJpaRepository.save(new ProductOptionEntity(
            null, testProduct.getId(), "기본옵션", 0L, 100L, now, now
        ));
    }

    @Test
    @DisplayName("주문 생성 성공")
    void createOrder_Success() throws Exception {
        List<OrderItemRequest> items = List.of(new OrderItemRequest(testProductOption.getId(), 2));
        CreateOrderRequest request = new CreateOrderRequest(testUser.getId(), items, null);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.totalAmount").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @DisplayName("주문 취소 성공")
    void cancelOrder_Success() throws Exception {
        // given - 주문 생성
        List<OrderItemRequest> items = new java.util.ArrayList<>();
        items.add(new OrderItemRequest(testProductOption.getId(), 1));
        CreateOrderRequest createRequest = new CreateOrderRequest(testUser.getId(), items, null);

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long orderId = objectMapper.readTree(createResponse).get("orderId").asLong();

        // when & then
        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("주문 완료 성공")
    void completeOrder_Success() throws Exception {
        // given - 주문 생성
        List<OrderItemRequest> items = new java.util.ArrayList<>();
        items.add(new OrderItemRequest(testProductOption.getId(), 1));
        CreateOrderRequest createRequest = new CreateOrderRequest(testUser.getId(), items, null);

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long orderId = objectMapper.readTree(createResponse).get("orderId").asLong();

        // when & then
        mockMvc.perform(post("/api/orders/{orderId}/complete", orderId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("존재하지 않는 주문 취소 실패")
    void cancelOrder_NotFound_Fail() throws Exception {
        mockMvc.perform(post("/api/orders/{orderId}/cancel", 99999L))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("존재하지 않는 주문 완료 실패")
    void completeOrder_NotFound_Fail() throws Exception {
        mockMvc.perform(post("/api/orders/{orderId}/complete", 99999L))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }
}
