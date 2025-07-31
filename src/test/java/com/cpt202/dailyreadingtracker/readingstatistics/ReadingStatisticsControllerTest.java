package com.cpt202.dailyreadingtracker.readingstatistics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadingStatisticsControllerTest {

    @Mock
    private ReadingStatisticsService readingStatisticsService;

    @InjectMocks
    private ReadingStatisticsController controller;

    private Principal mockPrincipal;
    private final Long testUserId = 1L;

    @BeforeEach
    void setUp() {
        mockPrincipal = () -> "test@test.com";
        when(readingStatisticsService.getUserIdFromPrincipal(mockPrincipal)).thenReturn(testUserId);
    }

    // RSC_001
    @Test
    public void testGetReadingStatisticsByPeriodTotal() {
        Map<String, Object> expectedStats = Map.of(
                "totalBooks", 5,
                "totalMinutes", 1200
        );

        when(readingStatisticsService.getReadingStatistics(testUserId)).thenReturn(expectedStats);

        Map<String, Object> result = controller.getReadingStatisticsByPeriod(mockPrincipal, "total");

        assertEquals(expectedStats, result);
        verify(readingStatisticsService).getReadingStatistics(testUserId);
        verify(readingStatisticsService, never()).getReadingStatisticsByPeriod(any(), any());
    }

    // RSC_002
    @Test
    public void testGetReadingStatisticsByPeriodWeekly() {
        Map<String, Object> expectedStats = Map.of(
                "weeklyBooks", 2,
                "weeklyMinutes", 300
        );

        when(readingStatisticsService.getReadingStatisticsByPeriod(testUserId, "weekly"))
                .thenReturn(expectedStats);

        Map<String, Object> result = controller.getReadingStatisticsByPeriod(mockPrincipal, "weekly");

        assertEquals(expectedStats, result);
        verify(readingStatisticsService).getReadingStatisticsByPeriod(testUserId, "weekly");
        verify(readingStatisticsService, never()).getReadingStatistics(any());
    }

    // RSC_003
    @Test
    public void testGetReadingStatisticsByDateRange() {
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);
        Map<String, Object> expectedStats = Map.of(
                "booksRead", 3,
                "minutesSpent", 450
        );

        when(readingStatisticsService.getReadingStatisticsByDateRange(testUserId, startDate, endDate))
                .thenReturn(expectedStats);

        Map<String, Object> result = controller.getReadingStatisticsByDateRange(
                mockPrincipal, startDate, endDate);

        assertEquals(expectedStats, result);
        verify(readingStatisticsService).getReadingStatisticsByDateRange(testUserId, startDate, endDate);
    }

    // RSC_004
    @Test
    public void testGetReadingStatisticsWithPeriod() {
        Map<String, Object> expectedStats = Map.of(
                "monthlyBooks", 4,
                "monthlyMinutes", 600
        );

        when(readingStatisticsService.getReadingStatisticsByPeriod(testUserId, "monthly"))
                .thenReturn(expectedStats);

        Map<String, Object> result = controller.getReadingStatistics(
                mockPrincipal, "monthly", null, null);

        assertEquals(expectedStats, result);
        verify(readingStatisticsService).getReadingStatisticsByPeriod(testUserId, "monthly");
    }

    // RSC_005
    @Test
    public void testGetReadingStatisticsWithDateRange() {
        LocalDate startDate = LocalDate.of(2023, 6, 1);
        LocalDate endDate = LocalDate.of(2023, 6, 30);
        Map<String, Object> expectedStats = Map.of(
                "booksRead", 2,
                "minutesSpent", 300
        );

        when(readingStatisticsService.getReadingStatisticsByDateRange(testUserId, startDate, endDate))
                .thenReturn(expectedStats);

        Map<String, Object> result = controller.getReadingStatistics(
                mockPrincipal, null, startDate, endDate);

        assertEquals(expectedStats, result);
        verify(readingStatisticsService).getReadingStatisticsByDateRange(testUserId, startDate, endDate);
    }

    // RSC_006
    @Test
    public void testGetReadingStatisticsWithNoParams() {
        Map<String, Object> expectedStats = Map.of(
                "totalBooks", 10,
                "totalMinutes", 1500
        );

        when(readingStatisticsService.getReadingStatistics(testUserId))
                .thenReturn(expectedStats);

        Map<String, Object> result = controller.getReadingStatistics(
                mockPrincipal, null, null, null);

        assertEquals(expectedStats, result);
        verify(readingStatisticsService).getReadingStatistics(testUserId);
    }

    // RSC_007
    @Test
    public void testGetReadingStatisticsWithTotalPeriod() {
        Map<String, Object> expectedStats = Map.of(
                "totalBooks", 8,
                "totalMinutes", 1200
        );

        when(readingStatisticsService.getReadingStatistics(testUserId))
                .thenReturn(expectedStats);

        Map<String, Object> result = controller.getReadingStatistics(
                mockPrincipal, "total", null, null);

        assertEquals(expectedStats, result);
        verify(readingStatisticsService).getReadingStatistics(testUserId);
    }

    // RSC_008
    @Test
    public void testGetReadingStatisticsWithInvalidPeriod() {
        Map<String, Object> expectedStats = Map.of(
                "customBooks", 1,
                "customMinutes", 100
        );

        when(readingStatisticsService.getReadingStatisticsByPeriod(testUserId, "invalid"))
                .thenReturn(expectedStats);

        Map<String, Object> result = controller.getReadingStatisticsByPeriod(
                mockPrincipal, "invalid");

        assertEquals(expectedStats, result);
        verify(readingStatisticsService).getReadingStatisticsByPeriod(testUserId, "invalid");
    }
}

