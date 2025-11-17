package com.hhplus.ecommerce.application.point;

import com.hhplus.ecommerce.domain.point.PointHistoryEntity;
import com.hhplus.ecommerce.domain.point.PointHistoryRepository;
import com.hhplus.ecommerce.presentation.user.res.PointHistoryItemResponse;
import com.hhplus.ecommerce.presentation.user.res.PointHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetUserPointHistoryUseCase {

    private final PointHistoryRepository pointHistoryRepository;

    public PointHistoryResponse execute(Long userId, int page, int size) {
        List<PointHistoryEntity> histories = pointHistoryRepository.findByUserId(userId);

        List<PointHistoryItemResponse> items = histories.stream()
            .map(this::toPointHistoryItemResponse)
            .collect(Collectors.toList());

        int totalElements = items.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return new PointHistoryResponse(
            items,
            totalElements,
            totalPages,
            size,
            page
        );
    }

    private PointHistoryItemResponse toPointHistoryItemResponse(PointHistoryEntity history) {
        return new PointHistoryItemResponse(
            history.getId(),
            history.getAmount().intValue(),
            history.getTransactionType().name(),
            history.getDescription(),
            formatTimestamp(System.currentTimeMillis())
        );
    }

    private String formatTimestamp(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .toString();
    }
}
