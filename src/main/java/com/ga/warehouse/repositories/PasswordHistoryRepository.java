package com.ga.warehouse.repositories;


import com.ga.warehouse.models.PasswordHistory;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {
    List<PasswordHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT ph FROM PasswordHistory ph WHERE ph.user.id = :userId ORDER BY ph.createdAt DESC")
    List<PasswordHistory> findRecentPasswordsByUserId(Long userId);

    long countByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordHistory ph WHERE ph.user.id = :userId AND ph.id NOT IN " +
            "(SELECT h.id FROM PasswordHistory h WHERE h.user.id = :userId ORDER BY h.createdAt DESC LIMIT 10)")
    int deleteOldPasswords(Long userId);
}
