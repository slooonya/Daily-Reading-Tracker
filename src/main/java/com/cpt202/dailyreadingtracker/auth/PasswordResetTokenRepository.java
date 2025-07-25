package com.cpt202.dailyreadingtracker.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cpt202.dailyreadingtracker.user.User;

import jakarta.transaction.Transactional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByUser(User user);
    Optional<PasswordResetToken> findByUserEmail(String email);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken t where t.token = :token")
    void deleteByToken(@Param("token") String token);    
}
