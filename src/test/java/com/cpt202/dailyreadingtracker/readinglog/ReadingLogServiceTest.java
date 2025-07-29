package com.cpt202.dailyreadingtracker.readinglog;

import java.time.LocalDate;
import java.util.*;

import com.cpt202.dailyreadingtracker.utils.EmailService;
import com.cpt202.dailyreadingtracker.violationlog.ViolationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.cpt202.dailyreadingtracker.role.Role;
import com.cpt202.dailyreadingtracker.user.User;
import com.cpt202.dailyreadingtracker.user.UserRepository;

import jakarta.persistence.EntityNotFoundException;


@ExtendWith(MockitoExtension.class)
class ReadingLogServiceTest {

    @Mock
    private ReadingLogRepository readingLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ViolationLogRepository violationLogRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ReadingLogService readingLogService;

    private User testUser;
    private ReadingLog testLog;
    private ReadingLogDto testLogDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");
        testUser.setRoles(new HashSet<>());
        testUser.getRoles().add(new Role("ROLE_USER"));

        testLog = new ReadingLog();
        testLog.setId(1L);
        testLog.setTitle("Test Book");
        testLog.setAuthor("Test Author");
        testLog.setDate(LocalDate.now());
        testLog.setTimeSpent(30);
        testLog.setCurrentPage(50);
        testLog.setTotalPages(100);
        testLog.setUser(testUser);
        testLog.setCurrent(true);

        testLogDto = new ReadingLogDto();
        testLogDto.setTitle("Test Book");
        testLogDto.setAuthor("Test Author");
        testLogDto.setDate(LocalDate.now());
        testLogDto.setTimeSpent(30);
        testLogDto.setCurrentPage(50);
        testLogDto.setTotalPages(100);
    }

    private User createAdminUser() {
        User admin = new User();
        admin.setId(2L);
        admin.setEmail("admin@test.com");
        admin.setRoles(new HashSet<>());
        admin.getRoles().add(new Role("ROLE_ADMIN"));
        return admin;
    }

    // RLS_001
    @Test
    public void testCreateLogWithNewBook() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(readingLogRepository.findByUserIdAndTitleIgnoreCaseAndIsCurrent(1L, "Test Book", true))
                .thenReturn(Collections.emptyList());
        when(readingLogRepository.save(any(ReadingLog.class))).thenReturn(testLog);

        ReadingLog result = readingLogService.createLog(1L, testLogDto);

        assertNotNull(result);
        assertEquals("Test Book", result.getTitle());
        verify(readingLogRepository).save(any(ReadingLog.class));
    }

    // RLS_002
    @Test
    public void testCreateLogWithExistingBookSamePageCount() {
        ReadingLog existingLog = new ReadingLog();
        existingLog.setTotalPages(100);
        existingLog.setCurrent(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(readingLogRepository.findByUserIdAndTitleIgnoreCaseAndIsCurrent(1L, "Test Book", true))
                .thenReturn(List.of(existingLog));
        when(readingLogRepository.save(any(ReadingLog.class))).thenReturn(testLog);

        ReadingLog result = readingLogService.createLog(1L, testLogDto);

        assertNotNull(result);
        assertFalse(existingLog.isCurrent());
        verify(readingLogRepository, times(2)).save(any(ReadingLog.class));
    }

    // RLS_003
    @Test
    public void testCreateLogWithPageCountMismatch() {
        ReadingLog existingLog = new ReadingLog();
        existingLog.setTotalPages(200);
        existingLog.setCurrent(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(readingLogRepository.findByUserIdAndTitleIgnoreCaseAndIsCurrent(1L, "Test Book", true))
                .thenReturn(List.of(existingLog));

        assertThrows(ResponseStatusException.class, () ->
                readingLogService.createLog(1L, testLogDto));
    }

    // RLS_004
    @Test
    public void testCreateLogWithNonexistentUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                readingLogService.createLog(1L, testLogDto));
    }

    // RLS_005
    @Test
    public void testUpdateLogWithValidData() {
        when(readingLogRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(readingLogRepository.save(any(ReadingLog.class))).thenReturn(testLog);

        ReadingLog result = readingLogService.updateLog(1L, 1L, testLogDto);

        assertNotNull(result);
        assertEquals("Test Book", result.getTitle());
        verify(readingLogRepository).save(testLog);
    }

    // RLS_006
    @Test
    public void testUpdateLogWithPageCountMismatch() {
        ReadingLog otherLog = new ReadingLog();
        otherLog.setTotalPages(200);

        when(readingLogRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(readingLogRepository.findByUserIdAndTitleIgnoreCaseAndAuthorIgnoreCaseAndIdNot(
                1L, "Test Book", "Test Author", 1L))
                .thenReturn(List.of(otherLog));

        assertThrows(ResponseStatusException.class, () ->
                readingLogService.updateLog(1L, 1L, testLogDto));
    }

    // RLS_007
    @Test
    public void testUpdateLogWithNonexistentLog() {
        when(readingLogRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                readingLogService.updateLog(1L, 1L, testLogDto));
    }

    // RLS_008
    @Test
    public void testUpdateLogByAdmin() {
        User adminUser = createAdminUser();

        when(readingLogRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        when(readingLogRepository.save(any(ReadingLog.class))).thenReturn(testLog);

        ReadingLog result = readingLogService.updateLog(2L, 1L, testLogDto);

        assertNotNull(result);
        verify(readingLogRepository).save(testLog);
    }

    // RLS_009
    @Test
    public void testDeleteLogByOwner() {
        when(readingLogRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        readingLogService.deleteLog(1L, 1L);

        verify(readingLogRepository).delete(testLog);
    }

    // RLS_010
    @Test
    public void testGetLogHistory() {
        ReadingLog olderLog = new ReadingLog();
        olderLog.setId(2L);
        olderLog.setTitle("Test Book");
        olderLog.setAuthor("Test Author");
        olderLog.setDate(LocalDate.now().minusDays(1));
        olderLog.setUser(testUser);
        olderLog.setCurrent(false);

        when(readingLogRepository.findByUserIdAndTitleIgnoreCaseAndAuthorIgnoreCase(1L, "test book", "test author"))
                .thenReturn(List.of(testLog, olderLog));

        verify(readingLogRepository, never()).save(any());

        List<ReadingLogHistoryDto> result = readingLogService.getLogHistory(1L, "Test Book", "Test Author", 1L);

        assertEquals(2, result.size());
        assertTrue(result.get(0).getDate().isAfter(result.get(1).getDate()));
    }

    // RLS_011
    @Test
    public void testGetLogByIdWithValidId() {
        when(readingLogRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        ReadingLog result = readingLogService.getLogById(1L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    // RLS_012
    @Test
    public void testGetLogByIdByAdmin() {
        User adminUser = createAdminUser();

        when(readingLogRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));

        ReadingLog result = readingLogService.getLogById(2L, 1L);

        assertNotNull(result);
    }
}
