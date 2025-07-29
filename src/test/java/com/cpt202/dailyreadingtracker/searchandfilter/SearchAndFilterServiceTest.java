package com.cpt202.dailyreadingtracker.searchandfilter;

import com.cpt202.dailyreadingtracker.readinglog.ReadingLog;
import com.cpt202.dailyreadingtracker.readinglog.ReadingLogRepository;
import com.cpt202.dailyreadingtracker.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchAndFilterServiceTest {

    @Mock
    private ReadingLogRepository readingLogRepository;

    @InjectMocks
    private SearchAndFilterService searchAndFilterService;

    private final Long testUserId = 1L;
    private List<ReadingLog> testLogs;

    @BeforeEach
    void setUp() {
        testLogs = List.of(
                createTestLog(1L, "Book 1", LocalDate.now().minusDays(2), 30),
                createTestLog(2L, "Book 2", LocalDate.now().minusDays(1), 45)
        );
    }

    // SFS_001
    @Test
    public void testSearchLogs() {
        String query = "book";
        when(readingLogRepository.searchByMultiFields(testUserId, query)).thenReturn(testLogs);

        List<ReadingLog> result = searchAndFilterService.searchLogs(testUserId, query);

        assertEquals(testLogs, result);
        verify(readingLogRepository).searchByMultiFields(testUserId, query);
    }

    // SFS_002
    @Test
    public void testFilterByDateRange() {
        LocalDate startDate = LocalDate.now().minusDays(3);
        LocalDate endDate = LocalDate.now();

        when(readingLogRepository.findByDateRange(testUserId, startDate, endDate))
                .thenReturn(testLogs);

        List<ReadingLog> result = searchAndFilterService.filterByDateRange(
                testUserId, startDate, endDate);

        assertEquals(testLogs, result);
        verify(readingLogRepository).findByDateRange(testUserId, startDate, endDate);
    }

    // SFS_003
    @Test
    public void testFilterByTimeRange() {
        int minTime = 20;
        int maxTime = 50;

        when(readingLogRepository.findByTimeSpentRange(testUserId, minTime, maxTime))
                .thenReturn(testLogs);

        List<ReadingLog> result = searchAndFilterService.filterByTimeRange(
                testUserId, minTime, maxTime);

        assertEquals(testLogs, result);
        verify(readingLogRepository).findByTimeSpentRange(testUserId, minTime, maxTime);
    }

    // SFS_004
    @Test
    public void testFilterLogsWithDateRange() {
        LocalDate startDate = LocalDate.now().minusDays(3);
        LocalDate endDate = LocalDate.now();

        when(readingLogRepository.findByFilters(testUserId, startDate, endDate, null, null))
                .thenReturn(testLogs);

        List<ReadingLog> result = searchAndFilterService.filterLogs(
                testUserId, startDate, endDate, null, null);

        assertEquals(testLogs, result);
        verify(readingLogRepository).findByFilters(testUserId, startDate, endDate, null, null);
    }

    // SFS_005
    @Test
    public void testFilterLogsWithTimeRange() {
        int minTime = 20;
        int maxTime = 50;

        when(readingLogRepository.findByFilters(testUserId, null, null, minTime, maxTime))
                .thenReturn(testLogs);

        List<ReadingLog> result = searchAndFilterService.filterLogs(
                testUserId, null, null, minTime, maxTime);

        assertEquals(testLogs, result);
        verify(readingLogRepository).findByFilters(testUserId, null, null, minTime, maxTime);
    }

    // SFS_006
    @Test
    public void testFilterLogsWithAllFilters() {
        LocalDate startDate = LocalDate.now().minusDays(3);
        LocalDate endDate = LocalDate.now();
        int minTime = 20;
        int maxTime = 50;

        when(readingLogRepository.findByFilters(testUserId, startDate, endDate, minTime, maxTime))
                .thenReturn(testLogs);

        List<ReadingLog> result = searchAndFilterService.filterLogs(
                testUserId, startDate, endDate, minTime, maxTime);

        assertEquals(testLogs, result);
        verify(readingLogRepository).findByFilters(testUserId, startDate, endDate, minTime, maxTime);
    }

    // SFS_007
    @Test
    public void testSearchLogsWithNoMatches() {
        when(readingLogRepository.searchByMultiFields(testUserId, "nonexistent"))
                .thenReturn(Collections.emptyList());

        List<ReadingLog> result = searchAndFilterService.searchLogs(testUserId, "nonexistent");

        assertTrue(result.isEmpty());
    }

    private ReadingLog createTestLog(Long id, String title, LocalDate date, int timeSpent) {
        ReadingLog log = new ReadingLog();
        log.setId(id);
        log.setTitle(title);
        log.setDate(date);
        log.setTimeSpent(timeSpent);
        log.setUser(new User());
        return log;
    }
}
