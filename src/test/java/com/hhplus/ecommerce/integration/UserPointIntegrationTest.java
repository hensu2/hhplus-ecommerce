package com.hhplus.ecommerce.integration;

import com.hhplus.ecommerce.domain.point.PointHistoryEntity;
import com.hhplus.ecommerce.domain.point.TransactionType;
import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.infrastructure.point.PointHistoryJpaRepository;
import com.hhplus.ecommerce.infrastructure.user.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserPointIntegrationTest {

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PointHistoryJpaRepository pointHistoryJpaRepository;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = UserEntity.create("integrationTestUser", 10000L, "USER");
        testUser = userJpaRepository.save(testUser);
    }

    @Test
    @DisplayName("사용자 포인트 충전 시나리오 테스트")
    void chargePointScenario() {
        // given
        Long initialPoint = testUser.getPoint();
        Long chargeAmount = 5000L;

        // when - 포인트 충전
        testUser.chargePoint(chargeAmount);
        userJpaRepository.save(testUser);

        // 포인트 내역 기록
        PointHistoryEntity history = PointHistoryEntity.createEarn(testUser, chargeAmount, "포인트 충전");
        pointHistoryJpaRepository.save(history);

        // then
        UserEntity updatedUser = userJpaRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getPoint()).isEqualTo(initialPoint + chargeAmount);

        List<PointHistoryEntity> histories = pointHistoryJpaRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getAmount()).isEqualTo(chargeAmount);
        assertThat(histories.get(0).getTransactionType()).isEqualTo(TransactionType.EARN);
    }

    @Test
    @DisplayName("사용자 포인트 사용 시나리오 테스트")
    void usePointScenario() {
        // given
        Long initialPoint = testUser.getPoint();
        Long useAmount = 3000L;

        // when - 포인트 사용
        testUser.usePoint(useAmount);
        userJpaRepository.save(testUser);

        // 포인트 내역 기록
        PointHistoryEntity history = PointHistoryEntity.createUse(testUser, useAmount, "상품 구매");
        pointHistoryJpaRepository.save(history);

        // then
        UserEntity updatedUser = userJpaRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getPoint()).isEqualTo(initialPoint - useAmount);

        List<PointHistoryEntity> histories = pointHistoryJpaRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getAmount()).isEqualTo(useAmount);
        assertThat(histories.get(0).getTransactionType()).isEqualTo(TransactionType.USE);
    }

    @Test
    @DisplayName("포인트 충전, 사용, 환불 전체 시나리오 테스트")
    void fullPointLifecycleScenario() {
        // given
        Long initialPoint = testUser.getPoint();

        // when - 1. 포인트 충전
        Long chargeAmount = 5000L;
        testUser.chargePoint(chargeAmount);
        userJpaRepository.save(testUser);
        pointHistoryJpaRepository.save(
                PointHistoryEntity.createEarn(testUser, chargeAmount, "첫 번째 충전")
        );

        // 2. 포인트 사용
        Long useAmount = 3000L;
        testUser.usePoint(useAmount);
        userJpaRepository.save(testUser);
        pointHistoryJpaRepository.save(
                PointHistoryEntity.createUse(testUser, useAmount, "상품 구매")
        );

        // 3. 포인트 환불
        Long refundAmount = 2000L;
        testUser.chargePoint(refundAmount); // 환불은 충전으로 처리
        userJpaRepository.save(testUser);
        pointHistoryJpaRepository.save(
                PointHistoryEntity.createRefund(testUser, refundAmount, "주문 취소 환불")
        );

        // then
        UserEntity finalUser = userJpaRepository.findById(testUser.getId()).orElseThrow();
        Long expectedPoint = initialPoint + chargeAmount - useAmount + refundAmount;
        assertThat(finalUser.getPoint()).isEqualTo(expectedPoint);

        List<PointHistoryEntity> histories = pointHistoryJpaRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());
        assertThat(histories).hasSize(3);
        assertThat(histories.get(0).getTransactionType()).isEqualTo(TransactionType.REFUND);
        assertThat(histories.get(1).getTransactionType()).isEqualTo(TransactionType.USE);
        assertThat(histories.get(2).getTransactionType()).isEqualTo(TransactionType.EARN);
    }

    @Test
    @DisplayName("여러 사용자의 포인트 내역이 독립적으로 관리된다")
    void multipleUsersIndependentHistory() {
        // given
        UserEntity user1 = UserEntity.create("user1", 1000L, "USER");
        UserEntity user2 = UserEntity.create("user2", 2000L, "USER");
        user1 = userJpaRepository.save(user1);
        user2 = userJpaRepository.save(user2);

        // when
        pointHistoryJpaRepository.save(PointHistoryEntity.createEarn(user1, 500L, "user1 충전"));
        pointHistoryJpaRepository.save(PointHistoryEntity.createEarn(user1, 300L, "user1 추가충전"));
        pointHistoryJpaRepository.save(PointHistoryEntity.createEarn(user2, 1000L, "user2 충전"));

        // then
        List<PointHistoryEntity> user1Histories = pointHistoryJpaRepository.findByUserIdOrderByCreatedAtDesc(user1.getId());
        List<PointHistoryEntity> user2Histories = pointHistoryJpaRepository.findByUserIdOrderByCreatedAtDesc(user2.getId());

        assertThat(user1Histories).hasSize(2);
        assertThat(user2Histories).hasSize(1);
        assertThat(user1Histories.get(0).getAmount()).isEqualTo(300L);
        assertThat(user2Histories.get(0).getAmount()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("포인트 사용 시 잔액 부족하면 예외가 발생한다")
    void insufficientPointThrowsException() {
        // given
        UserEntity poorUser = UserEntity.create("poorUser", 100L, "USER");
        poorUser = userJpaRepository.save(poorUser);

        // when & then
        UserEntity finalPoorUser = poorUser;
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> finalPoorUser.usePoint(1000L),
                "포인트가 부족합니다."
        );
    }

    @Test
    @DisplayName("포인트 내역 조회 시 최신순으로 정렬된다")
    void historyOrderedByLatest() throws InterruptedException {
        // given & when
        pointHistoryJpaRepository.save(PointHistoryEntity.createEarn(testUser, 100L, "첫번째"));
        Thread.sleep(10); // 시간 차이를 위해
        pointHistoryJpaRepository.save(PointHistoryEntity.createEarn(testUser, 200L, "두번째"));
        Thread.sleep(10);
        pointHistoryJpaRepository.save(PointHistoryEntity.createEarn(testUser, 300L, "세번째"));

        // then
        List<PointHistoryEntity> histories = pointHistoryJpaRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());
        assertThat(histories).hasSize(3);
        assertThat(histories.get(0).getDescription()).isEqualTo("세번째");
        assertThat(histories.get(1).getDescription()).isEqualTo("두번째");
        assertThat(histories.get(2).getDescription()).isEqualTo("첫번째");
    }
}