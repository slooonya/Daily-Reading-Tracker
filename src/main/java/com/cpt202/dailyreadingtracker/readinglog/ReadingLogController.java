package com.cpt202.dailyreadingtracker.readinglog;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller responsible for managing reading logs for individual users.
 * <p>
 * This controller provides endpoints for:
 * <ul>
 *     <li>Retrieving all reading logs for the authenticated user</li>
 *     <li>Creating, updating, and deleting reading logs for the authenticated user</li>
 *     <li>Retrieving the history of reading logs for specific titles and authors</li>
 * </ul>
 * <p>
 */

@RestController
@RequestMapping("/api/reading-logs")
@RequiredArgsConstructor
public class ReadingLogController {

    private final ReadingLogService readingLogService;
    
    @GetMapping
    public ResponseEntity<List<ReadingLog>> getAllLogs(Principal principal) {
        Long userId = readingLogService.getUserIdFromPrincipal(principal);
        List<ReadingLog> logs = readingLogService.getAllLogsByUser(userId);

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/{logId}")
    public ResponseEntity<?> getLogById(@PathVariable("logId") Long id, Principal principal) {
        if (id <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid log ID format"));
        }

        Long userId = readingLogService.getUserIdFromPrincipal(principal);

        try {
            ReadingLog log = readingLogService.getLogById(userId, id);
            return ResponseEntity.ok(log);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Reading log not found"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }
    }

    @PutMapping("/{logId}")
    public ResponseEntity<?> updateLog(@PathVariable("logId") Long id, @RequestBody @Valid ReadingLogDto dto, Principal principal) {
        Long userId = readingLogService.getUserIdFromPrincipal(principal);

        try {
            ReadingLog updatedLog = readingLogService.updateLog(userId, id, dto);
            return ResponseEntity.ok(Map.of("id", updatedLog.getId(), "message", "Reading log updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Reading log not found"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                "success", false, "error", e.getReason(), "code", e.getStatusCode().value()
            ));
        }
    }

    @PostMapping
    public ResponseEntity<?> createLog(@RequestBody @Valid ReadingLogDto dto, Principal principal) {
        if (dto == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Request body cannot be null"));
        }

        Long userId = readingLogService.getUserIdFromPrincipal(principal);

        try {
            ReadingLog log = readingLogService.createLog(userId, dto);
            return ResponseEntity.ok(Map.of("id", log.getId(), "message", "Reading log created successfully"));
        } catch (ResponseStatusException e) {
            if (e.getReason() != null && e.getReason().startsWith("PAGE_COUNT_MISMATCH")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", e.getReason()));
            }

            throw e;
        } catch (IllegalArgumentException e) {
            if ("Payload too large".equals(e.getMessage())) {
                return ResponseEntity.status(413).body(Map.of("error", "Payload too large"));
            }

            throw e;
        }
    }

    @DeleteMapping("/{logId}")
    public ResponseEntity<?> deleteLog(@PathVariable("logId") Long id, Principal principal) {
        Long userId = readingLogService.getUserIdFromPrincipal(principal);
        
        try {
            readingLogService.deleteLog(userId, id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Reading log not found"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<ReadingLogHistoryDto>> getLogHistory(@RequestParam String title,
                                    @RequestParam String author, @RequestParam Long currentLogId, Principal principal) {
        
        Long userId = readingLogService.getUserIdFromPrincipal(principal);
        List<ReadingLogHistoryDto> history = readingLogService
            .getLogHistory(userId, title, author, currentLogId);
        
        return ResponseEntity.ok(history);
    }
}