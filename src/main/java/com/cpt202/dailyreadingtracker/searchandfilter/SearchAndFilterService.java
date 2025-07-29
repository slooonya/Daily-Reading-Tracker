package com.cpt202.dailyreadingtracker.searchandfilter;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.cpt202.dailyreadingtracker.readinglog.ReadingLog;
import com.cpt202.dailyreadingtracker.readinglog.ReadingLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchAndFilterService {

    private final ReadingLogRepository readingLogRepository;

    public List<ReadingLog> searchLogs(Long userId, String query) {
        return readingLogRepository.searchByMultiFields(userId, query);
    }

    public List<ReadingLog> filterByDateRange(Long userId, LocalDate starDate, LocalDate endDate){
        return readingLogRepository.findByDateRange(userId, starDate, endDate);
    }

    public List<ReadingLog> filterByTimeRange(Long userId, int minTime, int maxTime) {
        return readingLogRepository.findByTimeSpentRange(userId, minTime, maxTime);
    }

    public List<ReadingLog> filterLogs(Long userId, LocalDate startDate, 
                                       LocalDate endDate, Integer minTime, Integer maxTime) {
        return readingLogRepository.findByFilters(userId, startDate, endDate, minTime, maxTime);
    }
}
