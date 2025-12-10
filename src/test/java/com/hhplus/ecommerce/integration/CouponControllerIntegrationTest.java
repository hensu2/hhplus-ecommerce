package com.hhplus.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.config.EmbeddedRedisConfig;
import com.hhplus.ecommerce.domain.coupon.CouponEntity;
import com.hhplus.ecommerce.domain.coupon.DiscountType;
import com.hhplus.ecommerce.infrastructure.coupon.jpa.CouponHistoryJpaRepository;
import com.hhplus.ecommerce.infrastructure.coupon.jpa.CouponJpaRepository;
import com.hhplus.ecommerce.presentation.coupon.req.ValidateCouponRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ContextConfiguration(initializers = EmbeddedRedisConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("CouponController 통합 테스트")
class CouponControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CouponHistoryJpaRepository couponHistoryJpaRepository;

    @Autowired
    private CouponJpaRepository couponJpaRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private CouponEntity testCoupon;

    @BeforeEach
    void setUp() {
        // 기존 히스토리 삭제
        couponHistoryJpaRepository.findByUserIdAndCouponId(1L, 1L).ifPresent(couponHistoryJpaRepository::delete);
        couponHistoryJpaRepository.findByUserIdAndCouponId(2L, 1L).ifPresent(couponHistoryJpaRepository::delete);

        // 테스트용 쿠폰 생성
        long now = System.currentTimeMillis();
        long validFrom = now - 86400000L; // 어제부터
        long validUntil = now + 86400000L; // 내일까지

        testCoupon = couponJpaRepository.save(new CouponEntity(
            null,
            "테스트 쿠폰",
            DiscountType.AMOUNT,
            5000,
            10000,
            50000,
            100,
            validFrom,
            validUntil,
            null,
            null
        ));

        // Redis에 재고 초기화
        String stockKey = "coupon:stock:" + testCoupon.getId();
        redisTemplate.opsForValue().set(stockKey, "100");
    }

    @Test
    @DisplayName("쿠폰 목록 조회 성공")
    void getCoupons_Success() throws Exception {
        mockMvc.perform(get("/api/coupons"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("쿠폰 발급 성공")
    void issueCoupon_Success() throws Exception {
        mockMvc.perform(post("/api/coupons/{couponId}/issue", testCoupon.getId())
                        .param("userId", "1"))
                .andDo(print())
                .andExpect(status().isAccepted())  // 비동기 처리로 202 반환
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.couponId").value(testCoupon.getId()))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("쿠폰 중복 발급 실패")
    void issueCoupon_Duplicate_Fail() throws Exception {
        // given - 첫 번째 발급
        mockMvc.perform(post("/api/coupons/{couponId}/issue", testCoupon.getId())
                        .param("userId", "1"))
                .andExpect(status().isAccepted());  // 비동기 처리로 202 반환

        // when & then - 중복 발급 시도
        mockMvc.perform(post("/api/coupons/{couponId}/issue", testCoupon.getId())
                        .param("userId", "1"))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("내 쿠폰 조회 성공")
    void getMyCoupons_Success() throws Exception {
        // given - 쿠폰 발급
        mockMvc.perform(post("/api/coupons/{couponId}/issue", testCoupon.getId())
                        .param("userId", "1"))
                .andExpect(status().isAccepted());  // 비동기 처리로 202 반환

        // when & then
        mockMvc.perform(get("/api/coupons/me")
                        .param("userId", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coupons").isArray());
    }

    @Test
    @DisplayName("쿠폰 유효성 검증 성공")
    void validateCoupon_Success() throws Exception {
        ValidateCouponRequest request = new ValidateCouponRequest(50000);

        mockMvc.perform(post("/api/coupons/{couponHistoryId}/validate", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").exists());
    }
}
