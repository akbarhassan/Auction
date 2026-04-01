package com.ga.warehouse.repositories;


import com.ga.warehouse.models.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByUserId(Long userId);

    List<PasswordResetToken> findByExpiresAtBefore(LocalDateTime now);

    @Query("SELECT t FROM EmailVerificationToken t WHERE t.token = :token AND t.expiresAt > :now AND t.used = false")
    Optional<PasswordResetToken> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);
}
