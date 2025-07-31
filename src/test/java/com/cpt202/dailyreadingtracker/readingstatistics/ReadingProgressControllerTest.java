package com.cpt202.dailyreadingtracker.readingstatistics;

import com.cpt202.dailyreadingtracker.user.User;
import com.cpt202.dailyreadingtracker.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.ui.Model;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadingProgressControllerTest {

    @Mock
    private ReadingStatisticsService readingStatisticsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Model model;

    @InjectMocks
    private ReadingProgressController controller;

    private Principal mockPrincipal;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockPrincipal = () -> "test@test.com";
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");
    }

    // RPC_001
    @Test
    public void testProgressPageWithValidUser() throws Exception {
        Map<String, Object> mockStats = Map.of(
                "bookCount", 5,
                "totalReadingTime", 1200,
                "avgDailyTime", 60
        );

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));
        when(readingStatisticsService.getReadingStatistics(1L)).thenReturn(mockStats);

        String viewName = controller.progressPage(model, mockPrincipal);

        assertEquals("readinglog/progress", viewName);
        verify(model).addAttribute("stats", mockStats);
        verify(model, never()).addAttribute(eq("error"), any());
    }

    // RPC_002
    @Test
    public void testProgressPageWithInvalidUser() throws Exception {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());

        String viewName = controller.progressPage(model, mockPrincipal);

        assertEquals("readinglog/progress", viewName);
        verify(model).addAttribute("error", "Failed to load statistics");
        verify(model).addAttribute("stats", Map.of(
                "bookCount", 0,
                "totalReadingTime", 0,
                "avgDailyTime", 0
        ));
    }

    // RPC_003
    @Test
    public void testProgressPageWhenServiceFails() throws Exception {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));
        when(readingStatisticsService.getReadingStatistics(1L))
                .thenThrow(new RuntimeException("Service error"));

        String viewName = controller.progressPage(model, mockPrincipal);

        assertEquals("readinglog/progress", viewName);
        verify(model).addAttribute("error", "Failed to load statistics");
        verify(model).addAttribute("stats", Map.of(
                "bookCount", 0,
                "totalReadingTime", 0,
                "avgDailyTime", 0
        ));
    }

    // RPC_004
    @Test
    public void testGetBookProgressWithValidUser() throws Exception {
        Map<String, Object> mockStats = Map.of(
                "booksInProgress", 3,
                "pagesRead", 150,
                "completionPercentage", 25.5
        );

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));
        when(readingStatisticsService.getBookProgressStats(1L)).thenReturn(mockStats);

        ResponseEntity<Map<String, Object>> response = controller.getBookProgress(mockPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockStats, response.getBody());
    }

    // RPC_005
    @Test
    public void testGetBookProgressWithInvalidUser() throws Exception {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            controller.getBookProgress(mockPrincipal);
        });
    }
}
