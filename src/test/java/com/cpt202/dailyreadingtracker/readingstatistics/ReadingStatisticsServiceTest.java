package com.cpt202.dailyreadingtracker.readingstatistics;

import com.cpt202.dailyreadingtracker.readinglog.ReadingLog;
import com.cpt202.dailyreadingtracker.readinglog.ReadingLogRepository;
import com.cpt202.dailyreadingtracker.user.User;
import com.cpt202.dailyreadingtracker.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadingStatisticsServiceTest {

    @Mock
    private ReadingLogRepository readingLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReadingStatisticsService readingStatisticsService;

    private final Long testUserId = 1L;
    private Principal mockPrincipal;

    @BeforeEach
    void setUp() {
        mockPrincipal = () -> "test@test.com";
    }

    // RSS_001
    @Test
    public void testGetReadingStatisticsWithEmptyLogs() {
        when(readingLogRepository.findByUserId(testUserId)).thenReturn(Collections.emptyList());

        Map<String, Object> result = readingStatisticsService.getReadingStatistics(testUserId);

        assertEquals(0, result.get("bookCount"));
        assertEquals(0, result.get("totalReadingTime"));
        assertEquals(0, result.get("avgDailyTime"));
        assertTrue(((List<?>) result.get("dates")).isEmpty());
        assertTrue(((List<?>) result.get("readingTimes")).isEmpty());
    }

    // RSS_002
    @Test
    public void testGetReadingStatisticsWithLogs() {
        List<ReadingLog> logs = List.of(
                createTestLog("Book 1", LocalDate.now().minusDays(2), 30),
                createTestLog("Book 1", LocalDate.now().minusDays(1), 45),
                createTestLog("Book 2", LocalDate.now(), 60)
        );

        when(readingLogRepository.findByUserId(testUserId)).thenReturn(logs);

        Map<String, Object> result = readingStatisticsService.getReadingStatistics(testUserId);

        assertEquals(2, result.get("bookCount"));
        assertEquals((long)(135), result.get("totalReadingTime"));
        assertEquals(45.0, result.get("avgDailyTime"));
        assertFalse(((List<?>) result.get("dates")).isEmpty());
        assertFalse(((List<?>) result.get("readingTimes")).isEmpty());
    }

    // RSS_003
    @Test
    public void testGetReadingStatisticsByPeriod() {
        LocalDate expectedStartDate = LocalDate.now().minusWeeks(1);
        List<ReadingLog> logs = List.of(
                createTestLog("Book 1", LocalDate.now().minusDays(2), 30),
                createTestLog("Book 2", LocalDate.now().minusDays(1), 45)
        );

        when(readingLogRepository.findByUserIdAndDateBetween(
                testUserId, expectedStartDate, LocalDate.now()))
                .thenReturn(logs);

        Map<String, Object> result = readingStatisticsService.getReadingStatisticsByPeriod(
                testUserId, "last_week");

        assertEquals(2, result.get("bookCount"));
        assertEquals((long)75, result.get("totalReadingTime"));
    }

    // RSS_004
    @Test
    public void testGetReadingStatisticsByDateRangeWithValidRange() {
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);
        List<ReadingLog> logs = List.of(
                createTestLog("Book 1", LocalDate.of(2025, 1, 15), 30),
                createTestLog("Book 2", LocalDate.of(2025, 1, 20), 45)
        );

        when(readingLogRepository.findByUserIdAndDateBetween(testUserId, startDate, endDate))
                .thenReturn(logs);

        Map<String, Object> result = readingStatisticsService.getReadingStatisticsByDateRange(
                testUserId, startDate, endDate);

        assertEquals(2, result.get("bookCount"));
        assertEquals((long)75, result.get("totalReadingTime"));
    }

    // RSS_005
    @Test
    public void testGetReadingStatisticsByDateRangeWithInvalidRange() {
        LocalDate startDate = LocalDate.of(2025, 2, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 1);

        assertThrows(IllegalArgumentException.class, () -> {
            readingStatisticsService.getReadingStatisticsByDateRange(
                    testUserId, startDate, endDate);
        });
    }

    // RSS_006
    @Test
    public void testGetBookProgressStats() {
        List<ReadingLog> logs = List.of(
                createTestLogWithPages("Book 1", 50, 200),
                createTestLogWithPages("Book 1", 100, 200), // Latest progress
                createTestLogWithPages("Book 2", 30, 100)
        );

        when(readingLogRepository.findByUserId(testUserId)).thenReturn(logs);

        Map<String, Object> result = readingStatisticsService.getBookProgressStats(testUserId);
        Map<String, Double> progress = (Map<String, Double>) result.get("bookProgress");

        assertEquals(2, progress.size());
        assertEquals(50.0, progress.get("Book 1")); // 100/200
        assertEquals(30.0, progress.get("Book 2")); // 30/100
    }

    // RSS_007
    @Test
    public void testGetUserIdFromPrincipalWithValidUser() {
        User user = new User();
        user.setId(testUserId);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        Long result = readingStatisticsService.getUserIdFromPrincipal(mockPrincipal);

        assertEquals(testUserId, result);
    }

    // RSS_008
    @Test
    public void testGetUserIdFromPrincipalWithInvalidUser() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());

        assertThrows(SecurityException.class, () -> {
            readingStatisticsService.getUserIdFromPrincipal(mockPrincipal);
        });
    }

    private ReadingLog createTestLog(String title, LocalDate date, int timeSpent) {
        ReadingLog log = new ReadingLog();
        log.setTitle(title);
        log.setDate(date);
        log.setTimeSpent(timeSpent);
        log.setUser(new User());
        return log;
    }

    private ReadingLog createTestLogWithPages(String title, int currentPage, int totalPages) {
        ReadingLog log = new ReadingLog();
        log.setTitle(title);
        log.setCurrentPage(currentPage);
        log.setTotalPages(totalPages);
        log.setDate(LocalDate.now());
        log.setUser(new User());
        return log;
    }
}
