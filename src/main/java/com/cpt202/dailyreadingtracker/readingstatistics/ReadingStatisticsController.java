package com.cpt202.dailyreadingtracker.readingstatistics;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * REST controller responsible for providing reading statistics for users.
 * <p>
 * Provides endpoints for:
 * <ul>
 *     <li>Fetching overall reading statistics</li>
 *     <li>Fetching statistics for specific time periods (e.g., daily, weekly, monthly)</li>
 *     <li>Fetching statistics for custom date ranges</li>
 * </ul>
 * <p>
 */

@RestController
@RequestMapping("/api/reading-statistics")
@RequiredArgsConstructor
public class ReadingStatisticsController {
    private final ReadingStatisticsService readingStatisticsService;

    @GetMapping("/by-period")
    public Map<String, Object> getReadingStatisticsByPeriod(Principal principal, @RequestParam String period) {
        Long userId = readingStatisticsService.getUserIdFromPrincipal(principal);

        if ("total".equals(period)) {
            return readingStatisticsService.getReadingStatistics(userId);
        }

        return readingStatisticsService.getReadingStatisticsByPeriod(userId, period);
    }

    @GetMapping("by-date-range")
    public Map<String, Object> getReadingStatisticsByDateRange(Principal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Long userId = readingStatisticsService.getUserIdFromPrincipal(principal);
        return readingStatisticsService.getReadingStatisticsByDateRange(userId, startDate, endDate);
    }

    @GetMapping
    public Map<String, Object> getReadingStatistics(Principal principal,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long userId = readingStatisticsService.getUserIdFromPrincipal(principal);

        if (period != null && !period.equals("total")) {
            return readingStatisticsService.getReadingStatisticsByPeriod(userId, period);
        } else if (startDate != null && endDate != null) {
            return readingStatisticsService.getReadingStatisticsByDateRange(userId, startDate, endDate);
        } else {
            return readingStatisticsService.getReadingStatistics(userId);
        }
    }

}