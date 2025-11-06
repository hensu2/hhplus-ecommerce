package com.hhplus.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.presentation.coupon.req.ValidateCouponRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("CouponController 통합 테스트")
class CouponControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("쿠폰 목록 조회 성공")
    void getCoupons_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/coupons"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("쿠폰 발급 성공")
    void issueCoupon_Success() throws Exception {
        // given
        Long userId = 1L;
        Long couponId = 1L;

        // when & then
        mockMvc.perform(post("/api/coupons/{couponId}/issue", couponId)
                        .param("userId", userId.toString()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.couponHistoryId").exists())
                .andExpect(jsonPath("$.couponId").exists())
                .andExpect(jsonPath("$.couponName").exists())
                .andExpect(jsonPath("$.discountType").exists())
                .andExpect(jsonPath("$.discountAmount").exists())
                .andExpect(jsonPath("$.issuedAt").exists());
    }

    @Test
    @DisplayName("내 쿠폰 조회 성공 - 전체")
    void getMyCoupons_All_Success() throws Exception {
        // given
        Long userId = 1L;

        // when & then
        mockMvc.perform(get("/api/coupons/me")
                        .param("userId", userId.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coupons").isArray());
    }

    @Test
    @DisplayName("내 쿠폰 조회 성공 - 상태 필터링")
    void getMyCoupons_Filtered_Success() throws Exception {
        // given
        Long userId = 1L;

        // when & then - ISSUED 상태
        mockMvc.perform(get("/api/coupons/me")
                        .param("userId", userId.toString())
                        .param("status", "ISSUED"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coupons").isArray());

        // when & then - USED 상태
        mockMvc.perform(get("/api/coupons/me")
                        .param("userId", userId.toString())
                        .param("status", "USED"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coupons").isArray());
    }

    @Test
    @DisplayName("쿠폰 유효성 검증 성공 - 사용 가능")
    void validateCoupon_Valid_Success() throws Exception {
        // given
        Long couponHistoryId = 1L;
        ValidateCouponRequest request = new ValidateCouponRequest(50000);

        // when & then
        mockMvc.perform(post("/api/coupons/{couponHistoryId}/validate", couponHistoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isValid").value(true))
                .andExpect(jsonPath("$.discountAmount").exists())
                .andExpect(jsonPath("$.finalAmount").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("쿠폰 유효성 검증 실패 - 최소 주문 금액 미달")
    void validateCoupon_BelowMinAmount_Invalid() throws Exception {
        // given
        Long couponHistoryId = 1L;
        ValidateCouponRequest request = new ValidateCouponRequest(5000);

        // when & then
        mockMvc.perform(post("/api/coupons/{couponHistoryId}/validate", couponHistoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isValid").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("쿠폰 유효성 검증 실패 - 존재하지 않는 쿠폰")
    void validateCoupon_NotFound_Invalid() throws Exception {
        // given
        Long nonExistentCouponHistoryId = 99999L;
        ValidateCouponRequest request = new ValidateCouponRequest(50000);

        // when & then
        mockMvc.perform(post("/api/coupons/{couponHistoryId}/validate", nonExistentCouponHistoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isValid").value(false))
                .andExpect(jsonPath("$.message").value("쿠폰을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("쿠폰 발급 후 내 쿠폰 목록에서 확인")
    void issueCouponAndGetMyCoupons_Success() throws Exception {
        // given
        Long userId = 2L;
        Long couponId = 1L;

        // when - 쿠폰 발급
        String issueResponse = mockMvc.perform(post("/api/coupons/{couponId}/issue", couponId)
                        .param("userId", userId.toString()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long couponHistoryId = objectMapper.readTree(issueResponse).get("couponHistoryId").asLong();

        // then - 내 쿠폰 목록 조회
        mockMvc.perform(get("/api/coupons/me")
                        .param("userId", userId.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coupons[?(@.couponHistoryId == " + couponHistoryId + ")]").exists());
    }

    @Test
    @DisplayName("쿠폰 할인 금액 계산 확인 - 퍼센트 할인")
    void validateCoupon_PercentDiscount_Success() throws Exception {
        // given - 10% 할인 쿠폰
        Long couponHistoryId = 1L;
        ValidateCouponRequest request = new ValidateCouponRequest(100000);

        // when & then - 10% 할인 = 10,000원, 최종 금액 = 90,000원
        String response = mockMvc.perform(post("/api/coupons/{couponHistoryId}/validate", couponHistoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isValid").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        int discountAmount = objectMapper.readTree(response).get("discountAmount").asInt();
        int finalAmount = objectMapper.readTree(response).get("finalAmount").asInt();

        assert discountAmount <= 5000 : "최대 할인 금액 5000원 제한";
        assert finalAmount == 100000 - discountAmount : "최종 금액 계산 오류";
    }

    @Test
    @DisplayName("쿠폰 할인 최대 금액 제한 확인")
    void validateCoupon_MaxDiscountLimit_Success() throws Exception {
        // given - 10% 할인 쿠폰 (최대 5000원)
        Long couponHistoryId = 1L;
        ValidateCouponRequest request = new ValidateCouponRequest(1000000); // 100만원

        // when & then - 10% = 100,000원이지만 최대 5000원으로 제한
        mockMvc.perform(post("/api/coupons/{couponHistoryId}/validate", couponHistoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isValid").value(true))
                .andExpect(jsonPath("$.discountAmount").value(lessThanOrEqualTo(5000)));
    }

    @Test
    @DisplayName("쿠폰 목록 조회 - 쿠폰 구조 검증")
    void getCoupons_Structure_Success() throws Exception {
        // when
        String response = mockMvc.perform(get("/api/coupons"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // then - 쿠폰이 있는 경우 구조 검증
        var coupons = objectMapper.readTree(response);
        if (coupons.size() > 0) {
            mockMvc.perform(get("/api/coupons"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].couponId").exists())
                    .andExpect(jsonPath("$[0].couponName").exists())
                    .andExpect(jsonPath("$[0].discountType").exists())
                    .andExpect(jsonPath("$[0].discountAmount").exists());
        }
    }

    @Test
    @DisplayName("내 쿠폰 조회 - 쿠폰 구조 검증")
    void getMyCoupons_Structure_Success() throws Exception {
        // given
        Long userId = 1L;

        // when
        String response = mockMvc.perform(get("/api/coupons/me")
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // then - 쿠폰이 있는 경우 구조 검증
        var coupons = objectMapper.readTree(response).get("coupons");
        if (coupons.size() > 0) {
            mockMvc.perform(get("/api/coupons/me")
                            .param("userId", userId.toString()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.coupons[0].couponHistoryId").exists())
                    .andExpect(jsonPath("$.coupons[0].couponId").exists())
                    .andExpect(jsonPath("$.coupons[0].couponName").exists())
                    .andExpect(jsonPath("$.coupons[0].status").exists())
                    .andExpect(jsonPath("$.coupons[0].issuedAt").exists());
        }
    }
}
