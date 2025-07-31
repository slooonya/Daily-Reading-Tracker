package com.cpt202.dailyreadingtracker.readingstatistics;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cpt202.dailyreadingtracker.readinglog.ReadingLog;
import com.cpt202.dailyreadingtracker.readinglog.ReadingLogRepository;
import com.cpt202.dailyreadingtracker.user.User;
import com.cpt202.dailyreadingtracker.user.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service responsible for generating reading statistics for users.
 * <ul>
 *     <li>Calculate total reading time, average daily time, and book count</li>
 *     <li>Generate statistics for specific time periods or date ranges</li>
 *     <li>Track progress for individual books</li>
 * </ul>
 */

@Service
@RequiredArgsConstructor
public class ReadingStatisticsService {
    private final ReadingLogRepository readingLogRepository;
    private final UserRepository userRepository;

    /**
     * Calculates overall reading statistics for a user.
     *
     * @param userId the ID of the user
     * @return a map containing reading statistics such as total reading time, book count, and average daily time
     */
    public Map<String, Object> getReadingStatistics(Long userId) {
        List<ReadingLog> allLogs = readingLogRepository.findByUserId(userId);

        if (allLogs.isEmpty()) {
            return Map.of(
                    "dates", List.of(),
                    "readingTimes", List.of(),
                    "bookCount", 0,
                    "totalReadingTime", 0,
                    "avgDailyTime", 0
            );
        }

        LocalDate firstDate = allLogs.stream()
                                      .map(ReadingLog::getDate)
                                      .min(LocalDate::compareTo)
                                      .orElseThrow();

        return calculateReadingStats(allLogs, firstDate, LocalDate.now());
    }

    /**
     * Calculates reading statistics for a user within a specific time period.
     *
     * @param userId the ID of the user
     * @param period the time period (e.g., "last_week", "last_month", "last_year")
     * @return a map containing reading statistics for the specified period
     */
    public Map<String, Object> getReadingStatisticsByPeriod(Long userId, String period) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = calculateStartDate(period);

        return getReadingStatisticsByDateRange(userId, startDate, endDate);
    }

    /**
     * Calculates reading statistics for a user within a specific date range.
     *
     * @param userId    the ID of the user
     * @param startDate the start date of the range
     * @param endDate   the end date of the range
     * @return a map containing reading statistics for the specified date range
     */
    public Map<String, Object> getReadingStatisticsByDateRange(Long userId,
                                                               LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        List<ReadingLog> logs = readingLogRepository.findByUserIdAndDateBetween(userId, startDate, endDate);

        return calculateReadingStats(logs, startDate, endDate);
    }

    /**
     * Helper method to calculate reading statistics for a given date range.
     *
     * @param logs      the list of reading logs
     * @param startDate the start date of the range
     * @param endDate   the end date of the range
     * @return a map containing calculated statistics
     */
    private Map<String, Object> calculateReadingStats(List<ReadingLog> logs,
                                                      LocalDate startDate,
                                                      LocalDate endDate) {
        List<String> dateList = startDate.datesUntil(endDate.plusDays(1))
                .map(date -> date.toString())
                .collect(Collectors.toList());

        Map<LocalDate, Long> dailyReading = logs.stream()
                .collect(Collectors.groupingBy(
                        ReadingLog::getDate,
                        Collectors.summingLong(ReadingLog::getTimeSpent)
                ));

        List<Long> timeList = startDate.datesUntil(endDate.plusDays(1))
                .map(date -> dailyReading.getOrDefault(date, 0L))
                .collect(Collectors.toList());

        return Map.of(
                "dates", dateList,
                "readingTimes", timeList,
                "bookCount", (int) logs.stream()
                        .map(log -> log.getTitle().toLowerCase().trim())
                        .distinct()
                        .count(),
                "totalReadingTime", timeList.stream().mapToLong(Long::longValue).sum(),
                "avgDailyTime", timeList.stream().mapToLong(Long::longValue).average().orElse(0)
        );
    }

     /**
     * Helper method to calculate the start date for a specific time period.
     *
     * @param period the time period (e.g., "last_week", "last_month")
     * @return the calculated start date
     */
    private LocalDate calculateStartDate(String period) {
        switch (period) {
            case "last_week": return LocalDate.now().minusWeeks(1);
            case "last_month": return LocalDate.now().minusMonths(1);
            case "last_three_months": return LocalDate.now().minusMonths(3);
            case "last_year": return LocalDate.now().minusYears(1);
            default: throw new IllegalArgumentException("Invalid time period: " + period);
        }
    }

    /**
     * Calculates progress for all books read by a user.
     *
     * @param userId the ID of the user
     * @return a map containing progress percentages for each book
     */
    public Map<String, Object> getBookProgressStats(Long userId) {
        List<ReadingLog> logs = readingLogRepository.findByUserId(userId);

        Map<String, Double> progressMap = logs.stream()
                .filter(log -> log.getTotalPages() != null && log.getTotalPages() > 0)
                .collect(Collectors.groupingBy(
                        ReadingLog::getTitle,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparingInt(ReadingLog::getCurrentPage)),
                                opt -> {
                                    ReadingLog log = opt.orElseThrow();
                                    return (log.getCurrentPage() * 100.0) / log.getTotalPages();
                                }
                        )));

        return Map.of("bookProgress", progressMap);
    }

    /**
     * Retrieves the user ID from the authenticated principal.
     *
     * @param principal the authenticated principal
     * @return the user ID
     */
    public Long getUserIdFromPrincipal(Principal principal) {
        String email = principal.getName(); 
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new SecurityException("User not found"));

        return user.getId();
    }
    
}
