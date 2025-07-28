package com.cpt202.dailyreadingtracker.violationlog;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import jakarta.transaction.Transactional;

@Repository
public interface ViolationLogRepository extends JpaRepository<ViolationLog, Long> {
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "false"))
    @Query("SELECT v FROM ViolationLog v ORDER BY v.date DESC")
    List<ViolationLog> findAllOrderByDateDesc();

    @Modifying
    @Transactional
    @Query("UPDATE ViolationLog v SET v.username = :newUsername WHERE v.user.id = :userId")
    void updateUsername(@Param("userId") Long userId, @Param("newUsername") String newUsername);

    List<ViolationLog> findByUserId(Long userId);
}
