package com.cpt202.dailyreadingtracker.searchandfilter;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cpt202.dailyreadingtracker.readinglog.ReadingLog;
import com.cpt202.dailyreadingtracker.readinglog.ReadingLogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reading-logs")
@RequiredArgsConstructor
public class SearchAndFilterController {

    private final ReadingLogService readingLogService;
    private final SearchAndFilterService searchAndFilterService;

    @GetMapping("/search")
    public ResponseEntity<List<ReadingLog>> searchLogs(Principal principal, @RequestParam String query) {
        Long userId = readingLogService.getUserIdFromPrincipal(principal);
        return ResponseEntity.ok(searchAndFilterService.searchLogs(userId, query));
    }
    
    @GetMapping("/filter/date")
    public ResponseEntity<List<ReadingLog>> filterByDateRange(Principal principal,
                                                    @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
                                                    @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate endDate) {
        Long userId = readingLogService.getUserIdFromPrincipal(principal);
        return ResponseEntity.ok(searchAndFilterService.filterByDateRange(userId, startDate, endDate));
    }
    
    @GetMapping("/filter/time")
    public ResponseEntity<List<ReadingLog>> filterByTimeRange(Principal principal, @RequestParam int min, 
                                                             @RequestParam int max) {
        Long userId = readingLogService.getUserIdFromPrincipal(principal);
        return ResponseEntity.ok(searchAndFilterService.filterByTimeRange(userId, min, max));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<ReadingLog>> filterLogs(Principal principal,
                                @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
                                @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate endDate,
                                @RequestParam(required = false) Integer minTime,
                                @RequestParam(required = false) Integer maxTime) {
        Long userId = readingLogService.getUserIdFromPrincipal(principal);
        List<ReadingLog> logs = searchAndFilterService.filterLogs(userId, startDate, endDate, minTime, maxTime);
        return ResponseEntity.ok(logs);
    }
}