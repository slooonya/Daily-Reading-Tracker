package com.cpt202.dailyreadingtracker.searchandfilter;

import com.cpt202.dailyreadingtracker.readinglog.ReadingLog;
import com.cpt202.dailyreadingtracker.readinglog.ReadingLogService;
import com.cpt202.dailyreadingtracker.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchAndFilterControllerTest {

    @Mock
    private ReadingLogService readingLogService;

    @Mock
    private SearchAndFilterService searchAndFilterService;

    @InjectMocks
    private SearchAndFilterController controller;

    private Principal mockPrincipal;
    private final Long testUserId = 1L;
    private List<ReadingLog> testLogs;

    @BeforeEach
    void setUp() {
        mockPrincipal = () -> "test@test.com";
        when(readingLogService.getUserIdFromPrincipal(mockPrincipal)).thenReturn(testUserId);

        testLogs = List.of(
                createTestLog(1L, "Book 1", LocalDate.now().minusDays(2), 30),
                createTestLog(2L, "Book 2", LocalDate.now().minusDays(1), 45)
        );
    }

    // SFC_001
    @Test
    public void testSearchLogs() throws Exception {
        String searchQuery = "book";
        when(searchAndFilterService.searchLogs(testUserId, searchQuery)).thenReturn(testLogs);

        ResponseEntity<List<ReadingLog>> response = controller.searchLogs(mockPrincipal, searchQuery);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testLogs, response.getBody());
        verify(searchAndFilterService).searchLogs(testUserId, searchQuery);
    }

    // SFC_002
    @Test
    public void testFilterByDateRange() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(3);
        LocalDate endDate = LocalDate.now();

        when(searchAndFilterService.filterByDateRange(testUserId, startDate, endDate))
                .thenReturn(testLogs);

        ResponseEntity<List<ReadingLog>> response = controller.filterByDateRange(
                mockPrincipal, startDate, endDate);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testLogs, response.getBody());
        verify(searchAndFilterService).filterByDateRange(testUserId, startDate, endDate);
    }

    // SFC_003
    @Test
    public void testFilterByTimeRange() throws Exception {
        int minTime = 20;
        int maxTime = 50;

        when(searchAndFilterService.filterByTimeRange(testUserId, minTime, maxTime))
                .thenReturn(testLogs);

        ResponseEntity<List<ReadingLog>> response = controller.filterByTimeRange(
                mockPrincipal, minTime, maxTime);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testLogs, response.getBody());
        verify(searchAndFilterService).filterByTimeRange(testUserId, minTime, maxTime);
    }

    // SFC_004
    @Test
    public void testFilterLogsWithDateRange() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(3);
        LocalDate endDate = LocalDate.now();

        when(searchAndFilterService.filterLogs(testUserId, startDate, endDate, null, null))
                .thenReturn(testLogs);

        ResponseEntity<List<ReadingLog>> response = controller.filterLogs(
                mockPrincipal, startDate, endDate, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testLogs, response.getBody());
        verify(searchAndFilterService).filterLogs(testUserId, startDate, endDate, null, null);
    }

    // SFC_005
    @Test
    public void testFilterLogsWithTimeRange() throws Exception {
        int minTime = 20;
        int maxTime = 50;

        when(searchAndFilterService.filterLogs(testUserId, null, null, minTime, maxTime))
                .thenReturn(testLogs);

        ResponseEntity<List<ReadingLog>> response = controller.filterLogs(
                mockPrincipal, null, null, minTime, maxTime);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testLogs, response.getBody());
        verify(searchAndFilterService).filterLogs(testUserId, null, null, minTime, maxTime);
    }

    // SFC_006
    @Test
    public void testFilterLogsWithAllParams() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(3);
        LocalDate endDate = LocalDate.now();
        int minTime = 20;
        int maxTime = 50;

        when(searchAndFilterService.filterLogs(testUserId, startDate, endDate, minTime, maxTime))
                .thenReturn(testLogs);

        ResponseEntity<List<ReadingLog>> response = controller.filterLogs(
                mockPrincipal, startDate, endDate, minTime, maxTime);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testLogs, response.getBody());
        verify(searchAndFilterService).filterLogs(testUserId, startDate, endDate, minTime, maxTime);
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

