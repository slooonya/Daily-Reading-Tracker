package com.cpt202.dailyreadingtracker.readinglog;

import java.security.Principal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ExtendWith(MockitoExtension.class)
class ReadingLogControllerTest {

    @Mock
    private ReadingLogService readingLogService;

    @InjectMocks
    private ReadingLogController controller;

    private MockMvc mockMvc;
    private Principal mockPrincipal;
    private final Long testUserId = 1L;
    private ReadingLog testLog;
    private ReadingLogDto testLogDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mockPrincipal = () -> "testuser@example.com";

        testLog = new ReadingLog();
        testLog.setId(1L);
        testLog.setTitle("Test Book");
        testLog.setAuthor("Test Author");

        testLogDto = new ReadingLogDto();
        testLogDto.setTitle("Test Book");
        testLogDto.setAuthor("Test Author");
        testLogDto.setTotalPages(6);
        testLogDto.setCurrentPage(0);
    }

    // RLC_001
    @Test
    public void testGetAllLogs() throws Exception {
        when(readingLogService.getAllLogsByUser(testUserId)).thenReturn(List.of(testLog));
        when(readingLogService.getUserIdFromPrincipal(mockPrincipal)).thenReturn(testUserId);

        mockMvc.perform(get("/api/reading-logs")
                        .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Test Book"));
    }

    // RLC_002
    @Test
    public void testGetLogByIdWithValidId() throws Exception {
        when(readingLogService.getLogById(testUserId, 1L)).thenReturn(testLog);
        when(readingLogService.getUserIdFromPrincipal(mockPrincipal)).thenReturn(testUserId);

        mockMvc.perform(get("/api/reading-logs/1")
                        .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Book"));
    }

    // RLC_003
    @Test
    public void testGetLogByIdWithInvalidId() throws Exception {
        mockMvc.perform(get("/api/reading-logs/0")
                        .principal(mockPrincipal))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid log ID format"));
    }

    // RLC_004
    @Test
    public void testGetLogByIdWhenNotFound() throws Exception {
        when(readingLogService.getLogById(testUserId, 1L)).thenThrow(new IllegalArgumentException());
        when(readingLogService.getUserIdFromPrincipal(mockPrincipal)).thenReturn(testUserId);

        mockMvc.perform(get("/api/reading-logs/1")
                        .principal(mockPrincipal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Reading log not found"));
    }

    // RLC_005
    @Test
    public void testUpdateLogWithValidData() throws Exception {
        ReadingLog updatedLog = new ReadingLog();
        updatedLog.setId(1L);
        updatedLog.setTitle("Updated Title");

        when(readingLogService.updateLog(anyLong(), anyLong(), any())).thenReturn(updatedLog);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/reading-logs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated Title\", \"author\":\"Author\", \"date\":\"2023-01-01\"}")
                        .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.message").exists());
    }

    // RLC_006
    @Test
    public void testDeleteLogWithValidId() throws Exception {
        doNothing().when(readingLogService).deleteLog(testUserId, 1L);
        when(readingLogService.getUserIdFromPrincipal(mockPrincipal)).thenReturn(testUserId);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/reading-logs/1")
                        .principal(mockPrincipal))
                .andExpect(status().isOk());
    }

    // RLC_007
    @Test
    public void testDeleteLogWhenNotFound() throws Exception {
        when(readingLogService.getUserIdFromPrincipal(mockPrincipal)).thenReturn(testUserId);

        doThrow(new IllegalArgumentException()).when(readingLogService).deleteLog(testUserId, 1L);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/reading-logs/1")
                        .principal(mockPrincipal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Reading log not found"));
    }

    // RLC_008
    @Test
    public void testGetLogHistory() throws Exception {
        ReadingLogHistoryDto historyDto = new ReadingLogHistoryDto();
        historyDto.setTitle("Test Book");
        historyDto.setAuthor("Test Author");

        when(readingLogService.getLogHistory(testUserId, "Test Book", "Test Author", 1L))
                .thenReturn(List.of(historyDto));
        when(readingLogService.getUserIdFromPrincipal(mockPrincipal)).thenReturn(testUserId);

        mockMvc.perform(get("/api/reading-logs/history")
                        .param("title", "Test Book")
                        .param("author", "Test Author")
                        .param("currentLogId", "1")
                        .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Book"))
                .andExpect(jsonPath("$[0].author").value("Test Author"));
    }

    // RLC_009
    @Test
    public void testGetUserIdFromPrincipalWhenUserNotFound() {
        Principal unknownPrincipal = () -> "unknown@example.com";
        when(readingLogService.getUserIdFromPrincipal(unknownPrincipal))
                .thenThrow(new SecurityException("User not found"));

        assertThrows(SecurityException.class, () ->
                controller.getLogById(1L, unknownPrincipal));
    }
}
