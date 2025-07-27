package com.cpt202.dailyreadingtracker.readinglog;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReadingLogRepository extends JpaRepository<ReadingLog, Long> {
    List<ReadingLog> findByUserId(Long userId);

    @Query("SELECT r FROM ReadingLog r ORDER BY r.date DESC")
    List<ReadingLog> findAllLogs();

    List<ReadingLog> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT r FROM ReadingLog r WHERE " +
    "(LOWER(r.title) LIKE LOWER(concat('%', :query, '%')) OR " +
    "LOWER(r.author) LIKE LOWER(concat('%', :query, '%')) OR " +
    "LOWER(r.notes) LIKE LOWER(concat('%', :query, '%'))) " + 
    "AND r.user.id = :userId")
    List<ReadingLog> searchByMultiFields(@Param("userId") Long userId, @Param("query") String query);

    @Query("SELECT r FROM ReadingLog r WHERE " +
    "r.user.id = :userId AND " +
    "r.date BETWEEN :startDate AND :endDate")
    List<ReadingLog> findByDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate starDate, 
                                     @Param("endDate") LocalDate endDate);

    @Query("SELECT r FROM ReadingLog r WHERE " +
    "r.user.id = :userId AND " +
    "r.timeSpent BETWEEN :minTime AND :maxTime")
    List<ReadingLog> findByTimeSpentRange(@Param("userId") Long userId, @Param("minTime") int minTime, 
                                          @Param("maxTime") int maxTime);

    @Query("SELECT r FROM ReadingLog r WHERE r.user.id = :userId " +
    "AND (:startDate IS NULL OR r.date >= :startDate) " +
    "AND (:endDate IS NULL OR r.date <= :endDate) " +
    "AND (:minTime IS NULL OR r.timeSpent >= :minTime) " +
    "AND (:maxTime IS NULL OR r.timeSpent <= :maxTime)")
    List<ReadingLog> findByFilters(@Param("userId") Long userId, @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate, @Param("minTime") Integer minTime,
                                   @Param("maxTime") Integer maxTime);

    @Query("SELECT r FROM ReadingLog r WHERE r.user.id = :userId AND LOWER(r.title) = LOWER(:title) AND r.isCurrent = :isCurrent")
    List<ReadingLog> findByUserIdAndTitleIgnoreCaseAndIsCurrent(@Param("userId") Long userId, 
                                                              @Param("title") String title,
                                                              @Param("isCurrent") boolean isCurrent);

    List<ReadingLog> findByTitleIgnoreCaseAndAuthorIgnoreCaseOrderByDateDesc(@Param("title") String title,
                                                                        @Param("author") String author);

    List<ReadingLog> findByUserIdAndTitleIgnoreCaseAndAuthorIgnoreCase(Long userId, String title, String author);
    List<ReadingLog> findByUserIdAndTitleIgnoreCaseAndAuthorIgnoreCaseAndIdNot(Long userId, String title, 
                                                                               String author, Long excludedId);

    @Query("SELECT r FROM ReadingLog r WHERE r.previousVersion.id = :previousVersionId")
    ReadingLog findByPreviousVersionId(@Param("previousVersionId") Long previousVersionId);

    @Query("SELECT r FROM ReadingLog r LEFT JOIN FETCH r.user ORDER BY r.date DESC")
    List<ReadingLog> findAllLogsWithUser();
}
