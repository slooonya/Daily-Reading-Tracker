package com.cpt202.dailyreadingtracker.searchandfilter;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.cpt202.dailyreadingtracker.readinglog.ReadingLog;
import com.cpt202.dailyreadingtracker.readinglog.ReadingLogRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service responsible for searching and filtering reading logs.
 * <ul>
 *     <li>Search logs by multiple fields (e.g., title, author, notes)</li>
 *     <li>Filter logs by date range</li>
 *     <li>Filter logs by time spent range</li>
 *     <li>Combine multiple filters to refine results</li>
 * </ul>
 */

@Service
@RequiredArgsConstructor
public class SearchAndFilterService {

    private final ReadingLogRepository readingLogRepository;

    /**
     * Searches reading logs for a user based on a query string.
     * The search is performed across multiple fields (e.g., title, author, notes).
     *
     * @param userId the ID of the user whose logs are being searched
     * @param query  the search query string
     * @return a list of reading logs matching the query
     */
    public List<ReadingLog> searchLogs(Long userId, String query) {
        return readingLogRepository.searchByMultiFields(userId, query);
    }

     /**
     * Filters reading logs for a user based on a date range.
     *
     * @param userId   the ID of the user whose logs are being filtered
     * @param startDate the start date of the range
     * @param endDate   the end date of the range
     * @return a list of reading logs within the specified date range
     */
    public List<ReadingLog> filterByDateRange(Long userId, LocalDate starDate, LocalDate endDate){
        return readingLogRepository.findByDateRange(userId, starDate, endDate);
    }

    /**
     * Filters reading logs for a user based on the range of time spent reading.
     *
     * @param userId  the ID of the user whose logs are being filtered
     * @param minTime the minimum time spent reading (in minutes)
     * @param maxTime the maximum time spent reading (in minutes)
     * @return a list of reading logs within the specified time range
     */
    public List<ReadingLog> filterByTimeRange(Long userId, int minTime, int maxTime) {
        return readingLogRepository.findByTimeSpentRange(userId, minTime, maxTime);
    }

     /**
     * Filters reading logs for a user based on multiple criteria, including date range and time spent range.
     *
     * @param userId    the ID of the user whose logs are being filtered
     * @param startDate the start date of the range
     * @param endDate   the end date of the range
     * @param minTime   the minimum time spent reading (optional)
     * @param maxTime   the maximum time spent reading (optional)
     * @return a list of reading logs matching the specified filters
     */
    public List<ReadingLog> filterLogs(Long userId, LocalDate startDate, 
                                       LocalDate endDate, Integer minTime, Integer maxTime) {
        return readingLogRepository.findByFilters(userId, startDate, endDate, minTime, maxTime);
    }
}
