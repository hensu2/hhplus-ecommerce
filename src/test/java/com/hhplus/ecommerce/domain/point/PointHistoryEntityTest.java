package com.hhplus.ecommerce.domain.point;

import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.infrastructure.point.PointHistoryJpaRepository;
import com.hhplus.ecommerce.infrastructure.user.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class PointHistoryEntityTest {

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PointHistoryJpaRepository pointHistoryJpaRepository;

    @Test
    @DisplayName("포인트 적립 내역을 생성하고 저장할 수 있다")
    void History() {
        // given
        UserEntity user = UserEntity.create("testuser", 0L, "USER");
        userJpaRepository.save(user);

        // when
        PointHistoryEntity history = PointHistoryEntity.createEarn(user, 1000L, "포인트 충전");
        PointHistoryEntity savedHistory = pointHistoryJpaRepository.save(history);

        // then
        assertThat(savedHistory.getId()).isNotNull();
        assertThat(savedHistory.getUser().getId()).isEqualTo(user.getId());
        assertThat(savedHistory.getAmount()).isEqualTo(1000L);
        assertThat(savedHistory.getTransactionType()).isEqualTo(TransactionType.EARN);
        assertThat(savedHistory.getDescription()).isEqualTo("포인트 충전");
        assertThat(savedHistory.getCreatedAt()).isNotNull();
        assertThat(savedHistory.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("포인트 사용 내역을 생성하고 저장할 수 있다")
    void createUseHistory() {
        // given
        UserEntity user = UserEntity.create("testuser", 5000L, "USER");
        userJpaRepository.save(user);

        // when
        PointHistoryEntity history = PointHistoryEntity.createUse(user, 2000L, "주문 결제");
        PointHistoryEntity savedHistory = pointHistoryJpaRepository.save(history);

        // then
        assertThat(savedHistory.getId()).isNotNull();
        assertThat(savedHistory.getAmount()).isEqualTo(2000L);
        assertThat(savedHistory.getTransactionType()).isEqualTo(TransactionType.USE);
        assertThat(savedHistory.getDescription()).isEqualTo("주문 결제");
    }

    @Test
    @DisplayName("포인트 환불 내역을 생성하고 저장할 수 있다")
    void createRefundHistory() {
        // given
        UserEntity user = UserEntity.create("testuser", 3000L, "USER");
        userJpaRepository.save(user);

        // when
        PointHistoryEntity history = PointHistoryEntity.createRefund(user, 1500L, "주문 취소");
        PointHistoryEntity savedHistory = pointHistoryJpaRepository.save(history);

        // then
        assertThat(savedHistory.getId()).isNotNull();
        assertThat(savedHistory.getAmount()).isEqualTo(1500L);
        assertThat(savedHistory.getTransactionType()).isEqualTo(TransactionType.REFUND);
        assertThat(savedHistory.getDescription()).isEqualTo("주문 취소");
    }

    @Test
    @DisplayName("사용자의 포인트 내역을 최신순으로 조회할 수 있다")
    void findByUserIdOrderByCreatedAtDesc() {
        // given
        UserEntity user = UserEntity.create("testuser", 0L, "USER");
        userJpaRepository.save(user);

        PointHistoryEntity history1 = PointHistoryEntity.createEarn(user, 1000L, "첫 번째 충전");
        PointHistoryEntity history2 = PointHistoryEntity.createEarn(user, 2000L, "두 번째 충전");
        PointHistoryEntity history3 = PointHistoryEntity.createUse(user, 500L, "사용");

        pointHistoryJpaRepository.save(history1);
        pointHistoryJpaRepository.save(history2);
        pointHistoryJpaRepository.save(history3);

        // when
        List<PointHistoryEntity> histories = pointHistoryJpaRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        // then
        assertThat(histories).hasSize(3);
        assertThat(histories.get(0).getDescription()).isEqualTo("사용"); // 최신
        assertThat(histories.get(1).getDescription()).isEqualTo("두 번째 충전");
        assertThat(histories.get(2).getDescription()).isEqualTo("첫 번째 충전"); // 가장 오래됨
    }

    @Test
    @DisplayName("포인트 금액이 0 이하일 경우 예외가 발생한다")
    void validateAmount() {
        // given
        UserEntity user = UserEntity.create("testuser", 0L, "USER");
        userJpaRepository.save(user);

        // when & then
        assertThatThrownBy(() -> PointHistoryEntity.createEarn(user, 0L, "잘못된 충전"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("포인트 금액은 0보다 커야 합니다.");

        assertThatThrownBy(() -> PointHistoryEntity.createEarn(user, -1000L, "잘못된 충전"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("포인트 금액은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("사용자 정보가 null일 경우 예외가 발생한다")
    void validateUser() {
        // when & then
        assertThatThrownBy(() -> PointHistoryEntity.createEarn(null, 1000L, "잘못된 충전"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 정보는 필수입니다.");
    }
}