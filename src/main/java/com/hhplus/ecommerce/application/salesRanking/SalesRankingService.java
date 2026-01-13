package com.hhplus.ecommerce.application.salesRanking;

import com.hhplus.ecommerce.domain.product.ProductEntity;
import com.hhplus.ecommerce.domain.salesRanking.SalesRankingItem;
import com.hhplus.ecommerce.infrastructure.product.ProductRepository;
import com.hhplus.ecommerce.infrastructure.salesRanking.SalesRankingRepository;
import com.hhplus.ecommerce.presentation.salesRanking.res.SalesRankingDto;
import com.hhplus.ecommerce.presentation.salesRanking.res.SalesRankingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SalesRankingService {

    private final SalesRankingRepository salesRankingRepository;
    private final ProductRepository productRepository;

    /**
     * 판매 랭킹 증가 (주문 생성 시)
     */
    public void increaseRanking(Long productId, Integer quantity, Long orderDate) {
        LocalDate date = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(orderDate),
            ZoneId.of("Asia/Seoul")
        ).toLocalDate();

        // 금일 랭킹 업데이트
        String dailyKey = generateDailyKey(date);
        salesRankingRepository.incrementScore(dailyKey, productId, quantity);
        salesRankingRepository.setExpire(dailyKey, 26, TimeUnit.HOURS);

        // 금주 랭킹 업데이트
        String weeklyKey = generateWeeklyKey(date);
        salesRankingRepository.incrementScore(weeklyKey, productId, quantity);
        salesRankingRepository.setExpire(weeklyKey, 8, TimeUnit.DAYS);
    }

    /**
     * 판매 랭킹 감소 (주문 취소 시)
     */
    public void decreaseRanking(Long productId, Integer quantity, Long cancelDate) {
        LocalDate date = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(cancelDate),
            ZoneId.of("Asia/Seoul")
        ).toLocalDate();

        // 금일 랭킹 차감
        String dailyKey = generateDailyKey(date);
        salesRankingRepository.decrementScore(dailyKey, productId, quantity);

        // 금주 랭킹 차감
        String weeklyKey = generateWeeklyKey(date);
        salesRankingRepository.decrementScore(weeklyKey, productId, quantity);
    }

    /**
     * 금일 판매 랭킹 조회 (페이징 + 상품 정보 조합)
     */
    public SalesRankingResponse getDailySalesRanking(LocalDate date, int page, int size) {
        String dailyKey = generateDailyKey(date);

        long offset = (long) page * size;
        long end = offset + size - 1;

        // Redis에서 페이징 조회
        List<SalesRankingItem> rankings = salesRankingRepository
            .getRankingWithScores(dailyKey, offset, end);

        // 상품 정보 조합
        List<SalesRankingDto> result = rankings.stream()
            .map(item -> {
                ProductEntity product = productRepository.getOrThrow(item.getProductId());
                return new SalesRankingDto(
                    item.getRank(),
                    product.getId(),
                    product.getProductName(),
                    product.getPrice(),
                    item.getSalesCount()
                );
            })
            .toList();

        long totalCount = salesRankingRepository.getSize(dailyKey);

        return new SalesRankingResponse(
            "daily",
            date.toString(),
            result,
            page,
            size,
            totalCount
        );
    }

    /**
     * 금주 판매 랭킹 조회 (페이징 + 상품 정보 조합)
     */
    public SalesRankingResponse getWeeklySalesRanking(int year, int week, int page, int size) {
        String weeklyKey = "sales:ranking:weekly:" + year + "-" + (week < 10 ? "0" + week : week);

        long offset = (long) page * size;
        long end = offset + size - 1;

        // Redis에서 페이징 조회
        List<SalesRankingItem> rankings = salesRankingRepository
            .getRankingWithScores(weeklyKey, offset, end);

        // 상품 정보 조합
        List<SalesRankingDto> result = rankings.stream()
            .map(item -> {
                ProductEntity product = productRepository.getOrThrow(item.getProductId());
                return new SalesRankingDto(
                    item.getRank(),
                    product.getId(),
                    product.getProductName(),
                    product.getPrice(),
                    item.getSalesCount()
                );
            })
            .toList();

        long totalCount = salesRankingRepository.getSize(weeklyKey);

        return new SalesRankingResponse(
            "weekly",
            year + "-W" + (week < 10 ? "0" + week : week),
            result,
            page,
            size,
            totalCount
        );
    }

    private String generateDailyKey(LocalDate date) {
        return "sales:ranking:daily:" + date.toString();
    }

    private String generateWeeklyKey(LocalDate date) {
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int year = date.get(weekFields.weekBasedYear());
        int week = date.get(weekFields.weekOfWeekBasedYear());
        return "sales:ranking:weekly:" + year + "-" + (week < 10 ? "0" + week : week);
    }
}