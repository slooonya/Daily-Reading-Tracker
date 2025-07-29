package com.cpt202.dailyreadingtracker.readinglog;

import java.security.Principal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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
@RequestMapping("/sorted_loglist_allusers")
@RequiredArgsConstructor
public class AllUserLogController {

    private final ReadingLogService readingLogService;
    private final UserRepository userRepository;
    private final ReadingLogRepository rlRepository;

    @GetMapping   
    public ResponseEntity<List<Map<String, Object>>> getAllUsersLogs(Principal principal) {
        List<ReadingLog> logs = rlRepository.findAllLogs();
        
        List<Map<String, Object>> response = logs.stream().map(log -> {
            Map<String, Object> logMap = new LinkedHashMap<>();
                logMap.put("id", log.getId());
                logMap.put("title", log.getTitle());
                logMap.put("author", log.getAuthor());
                logMap.put("createdAt", log.getCreatedAt());
                logMap.put("date", log.getDate());
                logMap.put("timeSpent", log.getTimeSpent());
                logMap.put("currentPage", log.getCurrentPage());
                logMap.put("totalPages", log.getTotalPages());
                logMap.put("notes", log.getNotes());
                logMap.put("userName", log.getUser().getUsername());
                
                return logMap;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{logId}")
    public ResponseEntity<?> updateLog(@PathVariable("logId") Long id, @RequestBody 
                                       @Valid ReadingLogDto dto, Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);

        try {
            ReadingLog updatedLog = readingLogService.updateLog(userId, id, dto);
            return ResponseEntity.ok(Map.of("id", updatedLog.getId(), "message", "Reading log updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Reading log not found"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }
    }

    @PostMapping
    public ResponseEntity<?> createLog(@RequestBody @Valid ReadingLogDto dto, Principal principal) {
        if (dto == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Request body cannot be null"));

        Long userId = getUserIdFromPrincipal(principal);

        try {
            ReadingLog log = readingLogService.createLog(userId, dto);
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
            readingLogService.deleteInappropriateLog(userId, id);    
            return ResponseEntity.ok(Map.of("message", "Reading log marked and deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Reading log not found"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }
    }

    @GetMapping("/{logId}")
    public ResponseEntity<?> getLogById(@PathVariable("logId") Long id, Principal principal) {
        if (id <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid log ID format"));
        }

        Long userId = getUserIdFromPrincipal(principal);
        try {
            ReadingLog log = readingLogService.getLogById(userId, id);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("id", log.getId());
            response.put("title", log.getTitle());
            response.put("author", log.getAuthor());
            response.put("createdAt", log.getCreatedAt());
            response.put("date", log.getDate());
            response.put("timeSpent", log.getTimeSpent());
            response.put("currentPage", log.getCurrentPage());
            response.put("totalPages", log.getTotalPages());
            response.put("notes", log.getNotes());
            response.put("userName", log.getUser().getUsername());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Reading log not found"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }
    }

    @GetMapping("/filter")
    public ResponseEntity<List<Map<String, Object>>> filterAllLogs(
        @RequestParam(required = false) String query,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) Integer minTime,
        @RequestParam(required = false) Integer maxTime) {
        
        List<ReadingLog> logs = rlRepository.findAllLogsWithUser();
        
        logs = applyFiltersInMemory(logs, query, startDate, endDate, minTime, maxTime);
        
        List<Map<String, Object>> response = logs.stream().map(log -> {
            Map<String, Object> logMap = new LinkedHashMap<>();
            logMap.put("id", log.getId());
            logMap.put("title", log.getTitle());
            logMap.put("author", log.getAuthor());
            logMap.put("date", log.getDate());
            logMap.put("timeSpent", log.getTimeSpent());
            logMap.put("currentPage", log.getCurrentPage());
            logMap.put("totalPages", log.getTotalPages());
            logMap.put("notes", log.getNotes());
            logMap.put("userName", log.getUser() != null ? log.getUser().getUsername() : "System");
            logMap.put("createdAt", log.getCreatedAt());
            return logMap;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    private List<ReadingLog> applyFiltersInMemory(List<ReadingLog> logs, String query, LocalDate startDate, 
                                                  LocalDate endDate, Integer minTime, Integer maxTime) {
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
                    (startDate == null || !log.getDate().isBefore(startDate)) &&
                    (endDate == null || !log.getDate().isAfter(endDate)))
                .collect(Collectors.toList());
        }
        
        if (minTime != null || maxTime != null) {
            logs = logs.stream()
                .filter(log -> 
                    (minTime == null || log.getTimeSpent() >= minTime) &&
                    (maxTime == null || log.getTimeSpent() <= maxTime))
                .collect(Collectors.toList());
        }
        
        return logs;
    }

    private Long getUserIdFromPrincipal(Principal principal) {
        String email = principal.getName(); 
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new SecurityException("User not found"));

        return user.getId();
    }
    
    @GetMapping("/history")
    public ResponseEntity<List<ReadingLog>> getAllUsersLogHistory(@RequestParam String title,
                                                                  @RequestParam String author, Principal principal) {
        List<ReadingLog> history = readingLogService.getAllLogHistory(title, author);

        return ResponseEntity.ok(history);
    }

}

