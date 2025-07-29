package com.cpt202.dailyreadingtracker.readinglog;

import com.cpt202.dailyreadingtracker.user.User;
import com.cpt202.dailyreadingtracker.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ExtendWith(MockitoExtension.class)
class AllUserLogControllerTest {

    @Mock
    private ReadingLogService readingLogService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReadingLogRepository readingLogRepository;

    @InjectMocks
    private AllUserLogController controller;

    private MockMvc mockMvc;
    private Principal mockPrincipal;
    private User testUser;
    private ReadingLog testLog;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mockPrincipal = () -> "testuser@test.com";

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("testuser@test.com");

        testLog = new ReadingLog();
        testLog.setId(1L);
        testLog.setTitle("Test Book");
        testLog.setAuthor("Test Author");
        testLog.setUser(testUser);
    }

    // ALC_001
    @Test
    void testGetAllUsersLogs() throws Exception {
        List<ReadingLog> logs = List.of(testLog);
        when(readingLogRepository.findAllLogs()).thenReturn(logs);

        mockMvc.perform(get("/sorted_loglist_allusers")
                        .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Book"))
                .andExpect(jsonPath("$[0].userName").value("testuser"));
    }

    // ALC_002
    @Test
    void testUpdateLogSuccess() throws Exception {
        ReadingLog updatedLog = new ReadingLog();
        updatedLog.setId(1L);
        updatedLog.setTitle("Updated Title");

        when(userRepository.findByEmail("testuser@test.com")).thenReturn(Optional.of(testUser));
        when(readingLogService.updateLog(anyLong(), anyLong(), any())).thenReturn(updatedLog);

        mockMvc.perform(MockMvcRequestBuilders.put("/sorted_loglist_allusers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated Title\", \"author\":\"Author\", \"date\":\"2023-01-01\"}")
                        .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.message").exists());
    }

    // ALC_003
    @Test
    void testGetLogByIdWithInvalidId() throws Exception {
        mockMvc.perform(get("/sorted_loglist_allusers/0")
                        .principal(mockPrincipal))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid log ID format"));
    }

    // ALC_004
    @Test
    void testFilterAllLogsWithQuery() throws Exception {
        List<ReadingLog> logs = List.of(testLog);
        when(readingLogRepository.findAllLogsWithUser()).thenReturn(logs);

        mockMvc.perform(get("/sorted_loglist_allusers/filter")
                        .queryParam("query", "Test")
                        .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Book"));
    }

    // ALC_005
    @Test
    void testGetAllUsersLogHistory() throws Exception {
        List<ReadingLog> history = List.of(testLog);
        when(readingLogService.getAllLogHistory(anyString(), anyString())).thenReturn(history);

        mockMvc.perform(get("/sorted_loglist_allusers/history")
                        .param("title", "Test")
                        .param("author", "Author")
                        .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Book"));
    }

    // ALC_006
    @Test
    void testGetUserIdFromPrincipalUserNotFound() {
        when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());
        Principal unknownPrincipal = () -> "nonexistent@test.com";

        assertThrows(SecurityException.class, () ->
                controller.getLogById(1L, unknownPrincipal));
    }

    // ALC_007
    @Test
    void testApplyFiltersInMemoryWithNullValues() throws Exception {
        List<ReadingLog> logs = List.of(testLog);
        @SuppressWarnings("unchecked")
        List<ReadingLog> result = (List<ReadingLog>) ReflectionTestUtils.invokeMethod(
                controller,
                "applyFiltersInMemory",
                logs, "Test", null, null, null, null
        );

        assertEquals(1, result.size());
        assertEquals("Test Book", result.get(0).getTitle());
    }
}