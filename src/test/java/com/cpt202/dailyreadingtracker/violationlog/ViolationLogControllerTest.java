package com.cpt202.dailyreadingtracker.violationlog;

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

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViolationLogControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ViolationLogRepository viorepo;

    @Mock
    private ViolationLogService vioService;

    @InjectMocks
    private ViolationLogController controller;

    private Principal mockPrincipal;
    private User testUser;
    private ViolationLog testLog;
    private ViolationLogDto testDto;

    @BeforeEach
    void setUp() {
        mockPrincipal = () -> "test@test.com";
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");

        testLog = new ViolationLog();
        testLog.setId(1L);
        testLog.setTitle("Test Violation");
        testLog.setCreatedAt(LocalDateTime.now());

        testDto = new ViolationLogDto();
        testDto.setTitle("Test Violation");
    }

    // VLC_001
    @Test
    public void testGetVioLogs() {
        List<ViolationLog> logs = List.of(testLog);
        when(viorepo.findAllOrderByDateDesc()).thenReturn(logs);

        ResponseEntity<List<ViolationLog>> response = controller.getVioLogs();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(1L, response.getBody().get(0).getId());
    }

    // VLC_002
    @Test
    public void testGetLogByIdWithValidId() {
        when(vioService.getLogById(1L, 1L)).thenReturn(testLog);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));

        ResponseEntity<?> response = controller.getLogById(1L, mockPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testLog, response.getBody());
    }

    // VLC_003
    @Test
    public void testGetLogByIdWithInvalidId() {
        ResponseEntity<?> response = controller.getLogById(0L, mockPrincipal);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid log ID format", ((Map<?, ?>) response.getBody()).get("error"));
    }

    // VLC_004
    @Test
    public void testGetLogByIdWithNonExistentLog() {
        when(vioService.getLogById(1L, 1L)).thenThrow(new IllegalArgumentException());
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));

        ResponseEntity<?> response = controller.getLogById(1L, mockPrincipal);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Violation log not found", ((Map<?, ?>) response.getBody()).get("error"));
    }

    // VLC_005
    @Test
    public void testUpdateLogWithValidData() {
        when(vioService.updateLog(1L, 1L, testDto)).thenReturn(testLog);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));

        ResponseEntity<?> response = controller.updateLog(1L, testDto, mockPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, ((Map<?, ?>) response.getBody()).get("id"));
    }

    // VLC_006
    @Test
    public void testCreateLogWithValidData() {
        when(vioService.createLog(1L, testDto)).thenReturn(testLog);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));

        ResponseEntity<?> response = controller.createLog(testDto, mockPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, ((Map<?, ?>) response.getBody()).get("id"));
    }

    // VLC_007
    @Test
    public void testCreateLogWithNullBody() {
        ResponseEntity<?> response = controller.createLog(null, mockPrincipal);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Request body cannot be null", ((Map<?, ?>) response.getBody()).get("error"));
    }

    // VLC_008
    @Test
    public void testDeleteLogWithValidId() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));
        ResponseEntity<?> response = controller.deleteLog(1L, mockPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Violation log deleted and restored successfully",
                ((Map<?, ?>) response.getBody()).get("message"));
    }

    // VLC_009
    @Test
    public void testFilterViolationLogsWithQuery() {
        ViolationLog matchingLog = new ViolationLog();
        matchingLog.setTitle("Matching Title");
        matchingLog.setAuthor("Author");
        matchingLog.setNotes("Some notes");

        ViolationLog nonMatchingLog = new ViolationLog();
        nonMatchingLog.setTitle("Other Title");
        nonMatchingLog.setAuthor("Other Author");
        nonMatchingLog.setNotes("Other notes");

        when(viorepo.findAll()).thenReturn(List.of(matchingLog, nonMatchingLog));

        ResponseEntity<List<ViolationLog>> response = controller.filterViolationLogs(
                "match", null, null, null, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Matching Title", response.getBody().get(0).getTitle());
    }

    // VLC_010
    @Test
    public void testFilterViolationLogsWithDateRange() {
        ViolationLog log1 = new ViolationLog();
        log1.setCreatedAt(LocalDateTime.of(2023, 6, 15, 0, 0));

        ViolationLog log2 = new ViolationLog();
        log2.setCreatedAt(LocalDateTime.of(2023, 7, 15, 0, 0));

        when(viorepo.findAll()).thenReturn(List.of(log1, log2));

        LocalDate startDate = LocalDate.of(2023, 7, 1);
        LocalDate endDate = LocalDate.of(2023, 7, 31);

        ResponseEntity<List<ViolationLog>> response = controller.filterViolationLogs(
                null, startDate, endDate, null, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    // VLC_011
    @Test
    public void testGetUserIdFromPrincipalWithInvalidUser() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());

        assertThrows(SecurityException.class, () -> {
            controller.getLogById(1L, mockPrincipal);
        });
    }
}