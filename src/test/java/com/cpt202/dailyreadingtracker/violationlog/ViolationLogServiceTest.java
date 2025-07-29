package com.cpt202.dailyreadingtracker.violationlog;

import com.cpt202.dailyreadingtracker.readinglog.ReadingLog;
import com.cpt202.dailyreadingtracker.readinglog.ReadingLogRepository;
import com.cpt202.dailyreadingtracker.role.Role;
import com.cpt202.dailyreadingtracker.user.User;
import com.cpt202.dailyreadingtracker.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViolationLogServiceTest {

    @Mock
    private ReadingLogRepository readingLogRepository;

    @Mock
    private ViolationLogRepository violationLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ViolationLogService violationLogService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private User testUser;
    private User adminUser;
    private ViolationLog testViolationLog;
    private ViolationLogDto testDto;
    private ReadingLog testReadingLog;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("user@test.com");
        testUser.setRoles(new HashSet<>());

        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setEmail("admin@test.com");
        adminUser.setRoles(Set.of(new Role("ROLE_ADMIN")));

        testViolationLog = new ViolationLog();
        testViolationLog.setId(1L);
        testViolationLog.setTitle("Test Violation");
        testViolationLog.setUser(testUser);

        testReadingLog = new ReadingLog();
        testReadingLog.setId(1L);
        testReadingLog.setTitle("Test Reading Log");

        testDto = new ViolationLogDto();
        testDto.setTitle("Updated Violation");
        testDto.setAuthor("Test Author");
    }

    // VLS_001
    @Test
    public void testCreateLogWithValidData() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(violationLogRepository.save(any(ViolationLog.class))).thenReturn(testViolationLog);

        ViolationLog result = violationLogService.createLog(1L, testDto);

        assertNotNull(result);
        assertEquals("Test Violation", result.getTitle());
        verify(violationLogRepository).save(any(ViolationLog.class));
    }

    // VLS_002
    @Test
    public void testCreateLogWithInvalidUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            violationLogService.createLog(1L, testDto);
        });
    }

    // VLS_003
    @Test
    public void testRestoreViolationLogAsNonAdmin() {
        when(securityContext.getAuthentication()).thenReturn(authentication);

        assertThrows(ResponseStatusException.class, () -> {
            violationLogService.restoreViolationLog(1L, 1L);
        });
    }

    // VLS_004
    @Test
    public void testUpdateLogAsAdmin() {
        when(violationLogRepository.findById(1L)).thenReturn(Optional.of(testViolationLog));
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        when(violationLogRepository.save(any(ViolationLog.class))).thenReturn(testViolationLog);

        ViolationLog result = violationLogService.updateLog(2L, 1L, testDto);

        assertEquals("Updated Violation", result.getTitle());
    }

    // VLS_005
    @Test
    public void testGetLogByIdAsAdmin() {
        when(violationLogRepository.findById(1L)).thenReturn(Optional.of(testViolationLog));
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));

        ViolationLog result = violationLogService.getLogById(2L, 1L);

        assertEquals("Test Violation", result.getTitle());
    }

    // VLS_006
    @Test
    public void testGetLogByIdAsNonAdmin() {
        when(violationLogRepository.findById(1L)).thenReturn(Optional.of(testViolationLog));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(SecurityException.class, () -> {
            violationLogService.getLogById(1L, 1L);
        });
    }

    // VLS_007
    @Test
    public void testGetLogByIdWithInvalidLog() {
        when(violationLogRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            violationLogService.getLogById(2L, 1L);
        });
    }

    // VLS_008
    @Test
    public void testUpdateLogWithPageCountMismatch() {
        testViolationLog.setUser(testUser);
        ReadingLog otherLog = new ReadingLog();
        otherLog.setTotalPages(100);

        when(violationLogRepository.findById(1L)).thenReturn(Optional.of(testViolationLog));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        testDto.setTotalPages(200);

        assertThrows(SecurityException.class, () -> {
            violationLogService.updateLog(1L, 1L, testDto);
        });
    }
}
