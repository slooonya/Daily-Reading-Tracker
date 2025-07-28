package com.cpt202.dailyreadingtracker.violationlog;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
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

import com.cpt202.dailyreadingtracker.user.User;
import com.cpt202.dailyreadingtracker.user.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/getviologs")
@RequiredArgsConstructor
public class ViolationLogController {

    private final UserRepository userRepository;
    private final ViolationLogRepository viorepo;
    private final ViolationLogService vioService;

    @GetMapping
    public ResponseEntity<List<ViolationLog>> getVioLogs() { 
        List<ViolationLog> logs = viorepo.findAllOrderByDateDesc();
        logs.forEach(log -> System.out.println("Log ID: " + log.getId()));
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/{logId}")
    public ResponseEntity<?> getLogById(@PathVariable("logId") Long id, Principal principal) {
        if (id <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid log ID format"));
        }

        Long userId = getUserIdFromPrincipal(principal);

        try {
            ViolationLog log = vioService.getLogById(userId, id);
            return ResponseEntity.ok(log);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Violation log not found"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }
    }

    @PutMapping("/{logId}")
    public ResponseEntity<?> updateLog(@PathVariable("logId") Long id, @RequestBody @Valid ViolationLogDto dto, Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);

        try {
            ViolationLog updatedLog = vioService.updateLog(userId, id, dto);
            return ResponseEntity.ok(Map.of("id", updatedLog.getId(), "message", "Violation log updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Violation log not found"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }
    }

    @PostMapping
    public ResponseEntity<?> createLog(@RequestBody @Valid ViolationLogDto dto, Principal principal) {
        if (dto == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Request body cannot be null"));
        }

        Long userId = getUserIdFromPrincipal(principal);

        try {
            ViolationLog log = vioService.createLog(userId, dto);
            return ResponseEntity.ok(Map.of("id", log.getId(), "message", "Reading log created successfully"));
        } catch (IllegalArgumentException e) {
            if ("Payload too large".equals(e.getMessage())) {
                return ResponseEntity.status(413).body(Map.of("error", "Payload too large"));
            }
            throw e;
        }
    }

    @DeleteMapping("/{logId}")
    public ResponseEntity<?> deleteLog(@PathVariable("logId") Long id, Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);

        try {
            vioService.restoreViolationLog(userId, id); 
            return ResponseEntity.ok(Map.of("message", "Violation log deleted and restored successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Violation log not found"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }
    }

    @GetMapping("/filter")
    public ResponseEntity<List<ViolationLog>> filterViolationLogs(
        @RequestParam(required = false) String query,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) Integer minTime,
        @RequestParam(required = false) Integer maxTime,
        @RequestParam(required = false) String sort) {

        List<ViolationLog> logs = viorepo.findAll();
        
        if (query != null && !query.isEmpty()) {
            String searchTerm = query.toLowerCase();
            logs = logs.stream()
                .filter(log -> 
                    log.getTitle().toLowerCase().contains(searchTerm) ||
                    log.getAuthor().toLowerCase().contains(searchTerm) ||
                    (log.getNotes() != null && log.getNotes().toLowerCase().contains(searchTerm)))
                .collect(Collectors.toList());
        }
        
        if (startDate != null || endDate != null) {
            logs = logs.stream()
                .filter(log -> 
                    (startDate == null || !log.getCreatedAt().toLocalDate().isBefore(startDate)) &&
                    (endDate == null || !log.getCreatedAt().toLocalDate().isAfter(endDate)))
                .collect(Collectors.toList());
        }
        
        if (minTime != null || maxTime != null) {
            logs = logs.stream()
                .filter(log -> 
                    (minTime == null || log.getTimeSpent() >= minTime) &&
                    (maxTime == null || log.getTimeSpent() <= maxTime))
                .collect(Collectors.toList());
        }
        
        return ResponseEntity.ok(logs);
    }

    private Long getUserIdFromPrincipal(Principal principal) {
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new SecurityException("User not found"));

        return user.getId();
    }
    
}

