package com.ga.warehouse.repositories;

import com.ga.warehouse.models.User;
import com.ga.warehouse.models.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUser(User user);

    Optional<UserProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("SELECT up FROM UserProfile up WHERE LOWER(up.fullName) LIKE LOWER(CONCAT('%', :name, '%'))")
    Optional<UserProfile> findByFullNameContainingIgnoreCase(String name);


}
