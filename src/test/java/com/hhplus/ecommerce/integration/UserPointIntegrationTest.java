package com.hhplus.ecommerce.integration;

import com.hhplus.ecommerce.application.point.ChargeUserPointUseCase;
import com.hhplus.ecommerce.application.point.GetUserPointHistoryUseCase;
import com.hhplus.ecommerce.application.point.GetUserPointUseCase;
import com.hhplus.ecommerce.domain.point.PointHistoryEntity;
import com.hhplus.ecommerce.domain.point.TransactionType;
import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.infrastructure.point.PointHistoryJpaRepository;
import com.hhplus.ecommerce.infrastructure.user.UserJpaRepository;
import com.hhplus.ecommerce.presentation.user.res.ChargePointResponse;
import com.hhplus.ecommerce.presentation.user.res.PointHistoryResponse;
import com.hhplus.ecommerce.presentation.user.res.PointResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 포인트 관리 통합 테스트
 * - Testcontainers를 사용한 실제 MySQL 환경 테스트
 * - Infrastructure 레이어 포함한 전체 플로우 검증
 */
@DisplayName("포인트 관리 통합 테스트")
class UserPointIntegrationTest extends TestContainerConfig {

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PointHistoryJpaRepository pointHistoryJpaRepository;

    @Autowired
    private GetUserPointUseCase getUserPointUseCase;

    @Autowired
    private ChargeUserPointUseCase chargeUserPointUseCase;

    @Autowired
    private GetUserPointHistoryUseCase getUserPointHistoryUseCase;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = UserEntity.create("testUser", 10000L, "USER");
        testUser = userJpaRepository.save(testUser);
    }

    @Test
    @DisplayName("포인트 조회 성공")
    @Transactional
    void getPoint_Success() {
        // when
        PointResponse response = getUserPointUseCase.execute(testUser.getId());

        // then
        assertThat(response.userId()).isEqualTo(testUser.getId());
        assertThat(response.username()).isEqualTo("testUser");
        assertThat(response.point()).isEqualTo(10000);
    }

    @Test
    @DisplayName("포인트 충전 성공")
    @Transactional
    void chargePoint_Success() {
        // given
        Integer chargeAmount = 5000;
        Long initialPoint = testUser.getPoint();

        // when
        ChargePointResponse response = chargeUserPointUseCase.execute(testUser.getId(), chargeAmount);

        // then
        assertThat(response.userId()).isEqualTo(testUser.getId());
        assertThat(response.amount()).isEqualTo(chargeAmount);
        assertThat(response.afterBalance()).isEqualTo(initialPoint + chargeAmount);
        assertThat(response.transactionType()).isEqualTo(TransactionType.EARN);

        // DB 검증
        UserEntity updatedUser = userJpaRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getPoint()).isEqualTo(initialPoint + chargeAmount);

        // 포인트 내역 검증
        List<PointHistoryEntity> histories = pointHistoryJpaRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getAmount()).isEqualTo(chargeAmount.longValue());
        assertThat(histories.get(0).getTransactionType()).isEqualTo(TransactionType.EARN);
    }

    @Test
    @DisplayName("포인트 충전 실패 - 최소 금액 미만")
    void chargePoint_Fail_BelowMinimum() {
        // given
        Integer chargeAmount = 500; // 최소 1000원 미만

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> chargeUserPointUseCase.execute(testUser.getId(), chargeAmount),
                "충전 금액은 1,000원 이상이어야 합니다."
        );
    }

    @Test
    @DisplayName("포인트 내역 조회 성공")
    @Transactional
    void getPointHistory_Success() {
        // given - 3번의 포인트 거래 발생
        chargeUserPointUseCase.execute(testUser.getId(), 5000);
        chargeUserPointUseCase.execute(testUser.getId(), 3000);
        chargeUserPointUseCase.execute(testUser.getId(), 2000);

        // when
        PointHistoryResponse response = getUserPointHistoryUseCase.execute(testUser.getId(), 0, 10);

        // then
        assertThat(response.getContent()).hasSize(3);
        assertThat(response.getNumber()).isEqualTo(0);
        assertThat(response.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("포인트 내역 페이징 검증")
    @Transactional
    void getPointHistory_Paging() {
        // given - 5번의 포인트 거래 발생
        for (int i = 0; i < 5; i++) {
            chargeUserPointUseCase.execute(testUser.getId(), 1000);
        }

        // when - 페이지당 2개씩 조회
        PointHistoryResponse page1 = getUserPointHistoryUseCase.execute(testUser.getId(), 0, 2);
        PointHistoryResponse page2 = getUserPointHistoryUseCase.execute(testUser.getId(), 1, 2);

        // then
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page2.getContent()).hasSize(2);
        assertThat(page1.getTotalPages()).isEqualTo(3); // 5개를 2개씩 = 3페이지
        assertThat(page1.getTotalElements()).isEqualTo(5);
    }

    @Test
    @DisplayName("포인트 충전, 사용, 환불 전체 시나리오")
    @Transactional
    void fullPointLifecycle() {
        // given
        Long initialPoint = testUser.getPoint();

        // when - 1. 포인트 충전
        ChargePointResponse chargeResponse = chargeUserPointUseCase.execute(testUser.getId(), 5000);
        assertThat(chargeResponse.afterBalance()).isEqualTo(initialPoint + 5000);

        // 2. 포인트 사용 (직접 도메인 메서드 호출)
        UserEntity user = userJpaRepository.findById(testUser.getId()).orElseThrow();
        user.usePoint(3000L);
        userJpaRepository.save(user);
        pointHistoryJpaRepository.save(PointHistoryEntity.createUse(user, 3000L, "상품 구매"));

        // 3. 포인트 환불
        chargeUserPointUseCase.execute(testUser.getId(), 2000);

        // then
        UserEntity finalUser = userJpaRepository.findById(testUser.getId()).orElseThrow();
        Long expectedPoint = initialPoint + 5000 - 3000 + 2000;
        assertThat(finalUser.getPoint()).isEqualTo(expectedPoint);

        // 포인트 내역 검증
        List<PointHistoryEntity> histories = pointHistoryJpaRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());
        assertThat(histories).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("여러 사용자의 포인트 내역이 독립적으로 관리된다")
    @Transactional
    void multipleUsersIndependentHistory() {
        // given
        UserEntity user1 = UserEntity.create("user1", 1000L, "USER");
        UserEntity user2 = UserEntity.create("user2", 2000L, "USER");
        user1 = userJpaRepository.save(user1);
        user2 = userJpaRepository.save(user2);

        // when
        chargeUserPointUseCase.execute(user1.getId(), 5000);
        chargeUserPointUseCase.execute(user1.getId(), 3000);
        chargeUserPointUseCase.execute(user2.getId(), 10000);

        // then
        List<PointHistoryEntity> user1Histories = pointHistoryJpaRepository.findByUserIdOrderByCreatedAtDesc(user1.getId());
        List<PointHistoryEntity> user2Histories = pointHistoryJpaRepository.findByUserIdOrderByCreatedAtDesc(user2.getId());

        assertThat(user1Histories).hasSize(2);
        assertThat(user2Histories).hasSize(1);
    }
}
