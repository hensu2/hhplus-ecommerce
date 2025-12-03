package com.hhplus.ecommerce.presentation.salesRanking;

import com.hhplus.ecommerce.application.salesRanking.SalesRankingService;
import com.hhplus.ecommerce.presentation.salesRanking.res.SalesRankingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.Locale;

@RestController
@RequestMapping("/api/sales-rankings")
@RequiredArgsConstructor
public class SalesRankingController {

    private final SalesRankingService salesRankingService;

    /**
     * 금일 판매 랭킹 조회
     * GET /api/sales-rankings/daily?date=2025-12-03&page=0&size=10
     */
    @GetMapping("/daily")
    public ResponseEntity<SalesRankingResponse> getDailySalesRanking(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now(ZoneId.of("Asia/Seoul"));
        SalesRankingResponse response = salesRankingService.getDailySalesRanking(targetDate, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * 금주 판매 랭킹 조회
     * GET /api/sales-rankings/weekly?year=2025&week=49&page=0&size=10
     */
    @GetMapping("/weekly")
    public ResponseEntity<SalesRankingResponse> getWeeklySalesRanking(
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) Integer week,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        LocalDate now = LocalDate.now(ZoneId.of("Asia/Seoul"));
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        int targetYear = (year != null) ? year : now.get(weekFields.weekBasedYear());
        int targetWeek = (week != null) ? week : now.get(weekFields.weekOfWeekBasedYear());

        SalesRankingResponse response = salesRankingService.getWeeklySalesRanking(
            targetYear, targetWeek, page, size
        );
        return ResponseEntity.ok(response);
    }
}