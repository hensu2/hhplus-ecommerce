package com.hhplus.ecommerce.application.point;

import com.hhplus.ecommerce.common.exception.UserNotFoundException;
import com.hhplus.ecommerce.domain.point.PointHistoryEntity;
import com.hhplus.ecommerce.domain.point.PointHistoryRepository;
import com.hhplus.ecommerce.domain.point.TransactionType;
import com.hhplus.ecommerce.domain.user.UserEntity;
import com.hhplus.ecommerce.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecordPointHistoryUseCase {

    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Transactional
    public PointHistoryEntity execute(Long userId, Long amount, TransactionType transactionType, String description) {
        UserEntity.validateUserId(userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        PointHistoryEntity pointHistory;
        if (transactionType == TransactionType.EARN) {
            pointHistory = PointHistoryEntity.createEarn(user, amount, description);
        } else if (transactionType == TransactionType.USE) {
            pointHistory = PointHistoryEntity.createUse(user, amount, description);
        } else if (transactionType == TransactionType.REFUND) {
            pointHistory = PointHistoryEntity.createRefund(user, amount, description);
        } else {
            throw new IllegalArgumentException("Invalid transaction type: " + transactionType);
        }

        return pointHistoryRepository.save(pointHistory);
    }
}