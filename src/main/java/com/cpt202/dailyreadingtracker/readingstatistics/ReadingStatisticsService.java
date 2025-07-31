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

@Service
@RequiredArgsConstructor
public class ReadingStatisticsService {
    private final ReadingLogRepository readingLogRepository;
    private final UserRepository userRepository;

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

    public Map<String, Object> getReadingStatisticsByPeriod(Long userId, String period) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = calculateStartDate(period);

        return getReadingStatisticsByDateRange(userId, startDate, endDate);
    }

    public Map<String, Object> getReadingStatisticsByDateRange(Long userId,
                                                               LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        List<ReadingLog> logs = readingLogRepository.findByUserIdAndDateBetween(userId, startDate, endDate);

        return calculateReadingStats(logs, startDate, endDate);
    }

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

    private LocalDate calculateStartDate(String period) {
        switch (period) {
            case "last_week": return LocalDate.now().minusWeeks(1);
            case "last_month": return LocalDate.now().minusMonths(1);
            case "last_three_months": return LocalDate.now().minusMonths(3);
            case "last_year": return LocalDate.now().minusYears(1);
            default: throw new IllegalArgumentException("Invalid time period: " + period);
        }
    }

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

    public Long getUserIdFromPrincipal(Principal principal) {
        String email = principal.getName(); 
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new SecurityException("User not found"));

        return user.getId();
    }
    
}

