package com.hhplus.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhplus.ecommerce.presentation.user.req.ChargePointRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UserPointController 통합 테스트")
class UserPointControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("포인트 조회 성공")
    void getPoint_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/users/me/point"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.point").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @DisplayName("포인트 충전 성공 - 최소 금액 이상")
    void chargePoint_Success() throws Exception {
        // given
        ChargePointRequest request = new ChargePointRequest(5000);

        // when & then
        mockMvc.perform(post("/api/users/me/point/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.amount").value(5000))
                .andExpect(jsonPath("$.afterBalance").exists())
                .andExpect(jsonPath("$.transactionType").value("EARN"))
                .andExpect(jsonPath("$.chargedAt").exists());
    }

    @Test
    @DisplayName("포인트 충전 성공 - 정확히 최소 금액")
    void chargePoint_MinimumAmount_Success() throws Exception {
        // given
        ChargePointRequest request = new ChargePointRequest(1000);

        // when & then
        mockMvc.perform(post("/api/users/me/point/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(1000))
                .andExpect(jsonPath("$.transactionType").value("EARN"));
    }

    @Test
    @DisplayName("포인트 충전 실패 - 최소 금액 미만")
    void chargePoint_BelowMinimum_Fail() throws Exception {
        // given
        ChargePointRequest request = new ChargePointRequest(999);

        // when & then
        mockMvc.perform(post("/api/users/me/point/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("포인트 충전 실패 - 0원")
    void chargePoint_ZeroAmount_Fail() throws Exception {
        // given
        ChargePointRequest request = new ChargePointRequest(0);

        // when & then
        mockMvc.perform(post("/api/users/me/point/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("포인트 충전 실패 - 음수")
    void chargePoint_NegativeAmount_Fail() throws Exception {
        // given
        ChargePointRequest request = new ChargePointRequest(-1000);

        // when & then
        mockMvc.perform(post("/api/users/me/point/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("포인트 이력 조회 성공 - 기본 페이징")
    void getPointHistory_DefaultPaging_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/users/me/point/history"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists())
                .andExpect(jsonPath("$.size").exists())
                .andExpect(jsonPath("$.number").exists());
    }

    @Test
    @DisplayName("포인트 이력 조회 성공 - 커스텀 페이징")
    void getPointHistory_CustomPaging_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/users/me/point/history")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @DisplayName("포인트 이력 조회 - 내역 구조 검증")
    void getPointHistory_ItemStructure_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/users/me/point/history"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].id").exists())
                .andExpect(jsonPath("$.content[*].amount").exists())
                .andExpect(jsonPath("$.content[*].transactionType").exists())
                .andExpect(jsonPath("$.content[*].description").exists())
                .andExpect(jsonPath("$.content[*].createdAt").exists());
    }

    @Test
    @DisplayName("포인트 충전 후 잔액 증가 확인")
    void chargePointAndCheckBalance_Success() throws Exception {
        // given - 현재 포인트 조회
        String beforeResponse = mockMvc.perform(get("/api/users/me/point"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        int beforePoint = objectMapper.readTree(beforeResponse).get("point").asInt();

        // when - 포인트 충전
        int chargeAmount = 10000;
        ChargePointRequest request = new ChargePointRequest(chargeAmount);

        mockMvc.perform(post("/api/users/me/point/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.afterBalance").value(beforePoint + chargeAmount));

        // then - 포인트 재조회하여 증가 확인
        mockMvc.perform(get("/api/users/me/point"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(beforePoint + chargeAmount));
    }

    @Test
    @DisplayName("포인트 충전 후 이력에 기록 확인")
    void chargePointAndCheckHistory_Success() throws Exception {
        // given - 현재 이력 개수 확인
        String beforeHistoryResponse = mockMvc.perform(get("/api/users/me/point/history"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        int beforeHistoryCount = objectMapper.readTree(beforeHistoryResponse).get("totalElements").asInt();

        // when - 포인트 충전
        ChargePointRequest request = new ChargePointRequest(3000);
        mockMvc.perform(post("/api/users/me/point/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // then - 이력 개수 증가 및 최신 이력 확인
        mockMvc.perform(get("/api/users/me/point/history"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(beforeHistoryCount + 1))
                .andExpect(jsonPath("$.content[0].amount").value(3000))
                .andExpect(jsonPath("$.content[0].transactionType").value("EARN"))
                .andExpect(jsonPath("$.content[0].description").value("포인트 충전"));
    }

    @Test
    @DisplayName("여러 번 포인트 충전 후 누적 잔액 확인")
    void multipleChargePointAndCheckBalance_Success() throws Exception {
        // given - 현재 포인트 조회
        String beforeResponse = mockMvc.perform(get("/api/users/me/point"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        int beforePoint = objectMapper.readTree(beforeResponse).get("point").asInt();

        // when - 여러 번 충전
        int[] chargeAmounts = {1000, 2000, 3000};
        int totalCharge = 0;

        for (int amount : chargeAmounts) {
            ChargePointRequest request = new ChargePointRequest(amount);
            mockMvc.perform(post("/api/users/me/point/charge")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
            totalCharge += amount;
        }

        // then - 최종 잔액 확인
        mockMvc.perform(get("/api/users/me/point"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(beforePoint + totalCharge));
    }

    @Test
    @DisplayName("대용량 포인트 충전 성공")
    void chargeLargeAmount_Success() throws Exception {
        // given
        ChargePointRequest request = new ChargePointRequest(1000000);

        // when & then
        mockMvc.perform(post("/api/users/me/point/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(1000000))
                .andExpect(jsonPath("$.transactionType").value("EARN"));
    }
}